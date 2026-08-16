package com.patechltd.salexfypos.backup;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;

import com.patechltd.salexfypos.db.AppDatabase;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.BackupLog;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.Prefs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class BackupManager {

    private BackupManager() {
    }

    public static File defaultBackupDir(Context context) {
        File dir = new File(context.getExternalFilesDir(null), "backups");
        if (!dir.exists()) dir.mkdirs();
        return dir;
    }

    public static boolean backupToFile(Context context, File target) {
        try {
            checkpoint(context);
            File db = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            if (!db.exists()) return false;
            copy(db, target);
            log(context, "BACKUP", "OK", target.getAbsolutePath(), "Created " + target.getName());
            return true;
        } catch (Exception e) {
            log(context, "BACKUP", "FAILED", null, e.getMessage());
            AppLogger.e("Backup failed", e);
            return false;
        }
    }

    public static boolean backupToUri(Context context, Uri target) {
        try {
            checkpoint(context);
            File db = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            if (!db.exists()) return false;
            try (OutputStream os = context.getContentResolver().openOutputStream(target, "w")) {
                if (os == null) return false;
                copy(db, os);
            }
            log(context, "BACKUP", "OK", target.toString(), "Created backup");
            return true;
        } catch (Exception e) {
            log(context, "BACKUP", "FAILED", null, e.getMessage());
            AppLogger.e("Backup to URI failed", e);
            return false;
        }
    }

    public static boolean restoreFromUri(Context context, Uri source) {
        File tmp = new File(context.getCacheDir(), "restore_" + System.currentTimeMillis() + ".db");
        try (InputStream is = context.getContentResolver().openInputStream(source)) {
            if (is == null) return false;
            try (OutputStream os = new FileOutputStream(tmp)) {
                byte[] buf = new byte[65536];
                int n;
                while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
            }
            return restoreFromFile(context, tmp);
        } catch (Exception e) {
            log(context, "RESTORE", "FAILED", null, e.getMessage());
            AppLogger.e("Restore failed", e);
            return false;
        }
    }

    public static boolean restoreFromFile(Context context, File source) {
        try {
            AppDatabase.destroyInstance();
            Repository.reset();
            File db = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            File wal = new File(db.getParent(), AppDatabase.DATABASE_NAME + "-wal");
            File shm = new File(db.getParent(), AppDatabase.DATABASE_NAME + "-shm");
            if (wal.exists()) wal.delete();
            if (shm.exists()) shm.delete();
            if (!db.getParentFile().exists()) db.getParentFile().mkdirs();
            copy(source, db);
            log(context, "RESTORE", "OK", source.getAbsolutePath(), "Database restored");
            return true;
        } catch (Exception e) {
            log(context, "RESTORE", "FAILED", null, e.getMessage());
            AppLogger.e("Restore failed", e);
            return false;
        }
    }

    public static boolean backupToDrive(Context context) {
        try {
            String uriString = Prefs.getString(context, Prefs.KEY_BACKUP_DRIVE_URI, null);
            if (uriString == null || uriString.isEmpty()) return false;
            checkpoint(context);
            File db = context.getDatabasePath(AppDatabase.DATABASE_NAME);
            if (!db.exists()) return false;
            Uri treeUri = Uri.parse(uriString);
            ContentResolver resolver = context.getContentResolver();

            String folderDocId = findChild(resolver, treeUri, DRIVE_FOLDER_NAME);
            Uri folderUri;
            if (folderDocId == null) {
                Uri rootDoc = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                        DocumentsContract.getTreeDocumentId(treeUri));
                Uri created = DocumentsContract.createDocument(resolver, rootDoc,
                        DocumentsContract.Document.MIME_TYPE_DIR, DRIVE_FOLDER_NAME);
                if (created == null) {
                    log(context, "DRIVE_BACKUP", "FAILED", uriString, "Could not create folder");
                    return false;
                }
                folderUri = created;
            } else {
                folderUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, folderDocId);
            }

            String name = "salexfy_backup_"
                    + DateUtil.iso(System.currentTimeMillis()).replaceAll("[^0-9]", "_") + ".db";
            Uri file = DocumentsContract.createDocument(resolver, folderUri,
                    "application/octet-stream", name);
            if (file == null) {
                log(context, "DRIVE_BACKUP", "FAILED", uriString, "Could not create file");
                return false;
            }
            try (OutputStream os = resolver.openOutputStream(file, "w")) {
                if (os == null) return false;
                copy(db, os);
            }

            List<DocEntry> children = listChildren(resolver, treeUri,
                    DocumentsContract.getDocumentId(folderUri));
            children.sort((a, b) -> {
                if (a.lastModified != 0 && b.lastModified != 0) {
                    return Long.compare(a.lastModified, b.lastModified);
                }
                return a.displayName.compareTo(b.displayName);
            });
            for (int i = 0; i < children.size() - 10; i++) {
                DocEntry e = children.get(i);
                try {
                    DocumentsContract.deleteDocument(resolver,
                            DocumentsContract.buildDocumentUriUsingTree(treeUri, e.docId));
                } catch (Exception ignored) {
                }
            }

            log(context, "DRIVE_BACKUP", "OK", folderUri.toString(), "Uploaded " + name);
            Prefs.putLong(context, Prefs.KEY_LAST_DRIVE_BACKUP, System.currentTimeMillis());
            return true;
        } catch (Exception e) {
            log(context, "DRIVE_BACKUP", "FAILED", null, e.getMessage());
            AppLogger.e("Drive backup failed", e);
            return false;
        }
    }

    private static final String DRIVE_FOLDER_NAME = "SalexfyBackups";

    private static String findChild(ContentResolver resolver, Uri treeUri, String displayName)
            throws Exception {
        String treeDocId = DocumentsContract.getTreeDocumentId(treeUri);
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, treeDocId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE
        };
        try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    if (displayName.equals(cursor.getString(
                            cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)))) {
                        return cursor.getString(
                                cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                    }
                }
            }
        }
        return null;
    }

    private static List<DocEntry> listChildren(ContentResolver resolver, Uri treeUri, String docId)
            throws Exception {
        List<DocEntry> result = new ArrayList<>();
        Uri childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId);
        String[] projection = {
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED
        };
        try (Cursor cursor = resolver.query(childrenUri, projection, null, null, null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    DocEntry e = new DocEntry();
                    e.docId = cursor.getString(
                            cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID));
                    e.displayName = cursor.getString(
                            cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME));
                    e.lastModified = cursor.getLong(
                            cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_LAST_MODIFIED));
                    result.add(e);
                }
            }
        }
        return result;
    }

    private static class DocEntry {
        String docId;
        String displayName;
        long lastModified;
    }

    private static void checkpoint(Context context) {
        try {
            AppDatabase db = AppDatabase.getInstance(context);
            db.getOpenHelper().getWritableDatabase().query("PRAGMA wal_checkpoint(FULL)").close();
        } catch (Exception ignored) {
        }
    }

    private static void copy(File from, File to) throws Exception {
        try (InputStream is = new FileInputStream(from); OutputStream os = new FileOutputStream(to)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
        }
    }

    private static void copy(File from, OutputStream os) throws Exception {
        try (InputStream is = new FileInputStream(from)) {
            byte[] buf = new byte[65536];
            int n;
            while ((n = is.read(buf)) != -1) os.write(buf, 0, n);
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
            if ("OK".equals(status)) {
                com.patechltd.salexfypos.util.Prefs.putLong(context,
                        com.patechltd.salexfypos.util.Prefs.KEY_LAST_BACKUP, System.currentTimeMillis());
            }
        } catch (Throwable ignored) {
        }
    }

    public static File timestampedBackupFile(Context context) {
        File dir = defaultBackupDir(context);
        return new File(dir, "salexfy_backup_" + DateUtil.iso(System.currentTimeMillis()).replaceAll("[^0-9]", "_") + ".db");
    }
}
