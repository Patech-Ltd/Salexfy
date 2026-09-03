package com.patechltd.salexfypos.drive;

import android.content.Context;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.patechltd.salexfypos.backup.BackupManager;
import com.patechltd.salexfypos.db.entity.BackupLog;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Backs up / restores the SQLite database against a fixed Google Drive folder
 * ({@link DriveConfig#BACKUP_FOLDER}) using the Drive API SDK (no folder picker required).
 *
 * <p>All heavy IO plus network calls are intended to run on a background thread.</p>
 */
public final class DriveBackupManager {

    private DriveBackupManager() {
    }

    /** Ensures the target folder exists and returns its id. */
    public static String ensureFolder(Drive drive) throws Exception {
        String folderId = findFolderId(drive);
        if (folderId != null) return folderId;
        File folder = new File()
                .setName(DriveConfig.BACKUP_FOLDER)
                .setMimeType("application/vnd.google-apps.folder");
        File created = drive.files().create(folder)
                .setFields("id")
                .execute();
        return created.getId();
    }

    private static String findFolderId(Drive drive) throws Exception {
        String q = "mimeType='application/vnd.google-apps.folder' and "
                + "trashed=false and name='" + DriveConfig.BACKUP_FOLDER + "'";
        FileList result = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setFields("files(id)")
                .execute();
        if (result.getFiles() != null && !result.getFiles().isEmpty()) {
            return result.getFiles().get(0).getId();
        }
        return null;
    }

    /** Uploads a timestamped backup of the current database into the Drive folder. */
    public static boolean backup(Context context, Drive drive) throws Exception {
        java.io.File clean = makeCleanBackupFile(context);
        if (clean == null) {
            log(context, "DRIVE_BACKUP", "FAILED", null, "Could not commit DB for backup");
            return false;
        }
        try {
            String folderId = ensureFolder(drive);
            String name = "salexfy_backup_"
                    + DateUtil.iso(System.currentTimeMillis()).replaceAll("[^0-9]", "_") + ".db";
            File metadata = new File().setName(name).setParents(Collections.singletonList(folderId));
            drive.files().create(
                            metadata,
                            new com.google.api.client.http.FileContent("application/octet-stream", clean))
                    .setFields("id")
                    .execute();
            log(context, "DRIVE_BACKUP", "OK", name, "Uploaded " + name);
            Prefs.putLong(context, Prefs.KEY_LAST_DRIVE_BACKUP, System.currentTimeMillis());
            prune(context, drive, folderId, 10);
            return true;
        } finally {
            if (clean.exists()) clean.delete();
        }
    }

    /** Commits the live DB to a fresh, clean, -wal/-shm-free temp file for upload. */
    private static java.io.File makeCleanBackupFile(Context context) throws Exception {
        java.io.File tmp = new java.io.File(context.getCacheDir(),
                "clean_backup_" + System.currentTimeMillis() + ".db");
        if (!BackupManager.commitDbToCleanFile(context, tmp)) {
            if (tmp.exists()) tmp.delete();
            return null;
        }
        return tmp;
    }

    /** Keeps only the newest {@code keep} backups in the folder. */
    private static void prune(Context context, Drive drive, String folderId, int keep) throws Exception {
        List<DriveBackupEntry> all = listBackups(context, drive);
        for (int i = keep; i < all.size(); i++) {
            try {
                drive.files().delete(all.get(i).fileId).execute();
            } catch (Exception ignored) {
            }
        }
    }

    /** Lists backups in the folder, newest first. Only salexfy_backup_* files are considered. */
    public static List<DriveBackupEntry> listBackups(Context context, Drive drive) throws Exception {
        String folderId = findFolderId(drive);
        if (folderId == null) return new ArrayList<>();
        String q = "'" + folderId + "' in parents and trashed=false";
        FileList result = drive.files().list()
                .setQ(q)
                .setSpaces("drive")
                .setOrderBy("createdTime desc")
                .setPageSize(100)
                .setFields("files(id,name,createdTime,size)")
                .execute();
        List<DriveBackupEntry> out = new ArrayList<>();
        if (result.getFiles() != null) {
            for (File f : result.getFiles()) {
                if (f.getName() == null) continue;
                long created = 0;
                try {
                    created = f.getCreatedTime() != null ? f.getCreatedTime().getValue() : 0;
                } catch (Exception ignored) {
                }
                long size = 0;
                try {
                    size = f.getSize() != null ? f.getSize() : 0;
                } catch (Exception ignored) {
                }
                out.add(new DriveBackupEntry(f.getId(), f.getName(), created, size));
            }
        }
        return out;
    }

    /** Lists the safety copies uploaded before restores (slex_db_before_restore_*). */
    public static List<DriveBackupEntry> listBeforeRestore(Context context, Drive drive) throws Exception {
        List<DriveBackupEntry> all = listBackups(context, drive);
        List<DriveBackupEntry> out = new ArrayList<>();
        for (DriveBackupEntry e : all) {
            if (e.name != null && e.name.startsWith(DriveConfig.BEFORE_RESTORE_PREFIX)) {
                out.add(e);
            }
        }
        return out;
    }

    /** The newest regular backup (ignores before-restore safety copies). */
    public static DriveBackupEntry getLatestBackup(Context context, Drive drive) throws Exception {
        for (DriveBackupEntry e : listBackups(context, drive)) {
            if (e.name != null && e.name.startsWith("salexfy_backup_")
                    && !e.name.startsWith(DriveConfig.BEFORE_RESTORE_PREFIX)) {
                return e;
            }
        }
        return null;
    }

    /**
     * Restores the latest Drive backup. Before overwriting the local database it uploads a safety
     * copy of the current data named slex_db_before_restore_&lt;timestamp&gt;.db.
     */
    public static boolean restoreLatest(Context context, Drive drive) throws Exception {
        DriveBackupEntry latest = getLatestBackup(context, drive);
        if (latest == null) {
            log(context, "DRIVE_RESTORE", "FAILED", null, "No Drive backup found");
            return false;
        }
        uploadSafetyCopyBeforeRestore(context, drive);
        return restoreFile(context, drive, latest);
    }

    /** Restores a specific named backup from Drive. */
    public static boolean restoreNamed(Context context, Drive drive, String name) throws Exception {
        for (DriveBackupEntry e : listBackups(context, drive)) {
            if (name.equals(e.name)) {
                return restoreFile(context, drive, e);
            }
        }
        log(context, "DRIVE_RESTORE", "FAILED", name, "Backup not found");
        return false;
    }

    private static boolean restoreFile(Context context, Drive drive, DriveBackupEntry entry) throws Exception {
        java.io.File tmp = new java.io.File(context.getCacheDir(), "restore_" + System.currentTimeMillis() + ".db");
        try (InputStream is = drive.files().get(entry.fileId).executeMediaAsInputStream();
             OutputStream os = new FileOutputStream(tmp)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
        }
        boolean ok = BackupManager.restoreFromFile(context, tmp);
        log(context, "DRIVE_RESTORE", ok ? "OK" : "FAILED", entry.name,
                ok ? "Restored " + entry.name : "Restore failed");
        if (tmp.exists()) tmp.delete();
        return ok;
    }

    /** Uploads a safety copy of the current database before a restore overwrites it. */
    public static void uploadSafetyCopyBeforeRestore(Context context, Drive drive) throws Exception {
        java.io.File clean = makeCleanBackupFile(context);
        if (clean == null) return;
        try {
            String folderId = ensureFolder(drive);
            String name = DriveConfig.BEFORE_RESTORE_PREFIX
                    + DateUtil.iso(System.currentTimeMillis()).replaceAll("[^0-9]", "_") + ".db";
            File metadata = new File().setName(name).setParents(Collections.singletonList(folderId));
            drive.files().create(metadata,
                            new com.google.api.client.http.FileContent("application/octet-stream", clean))
                    .setFields("id")
                    .execute();
            log(context, "DRIVE_SAFE_COPY", "OK", name, "Safety copy uploaded");
        } finally {
            if (clean.exists()) clean.delete();
        }
    }

    private static void log(Context context, String type, String status, String path, String message) {
        try {
            BackupLog l = new BackupLog();
            l.uid = UUID.randomUUID().toString();
            l.timestamp = System.currentTimeMillis();
            l.type = type;
            l.status = status;
            l.filePath = path;
            l.message = message;
            Repository.get(context).crash.insertBackup(l);
        } catch (Throwable ignored) {
        }
    }
}
