package com.patechltd.salexfypos.backup;

import android.content.Context;
import android.net.Uri;

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
            boolean ok = commitDbToCleanFile(context, target);
            if (!ok) return false;
            log(context, "BACKUP", "OK", target.getAbsolutePath(), "Created " + target.getName());
            return true;
        } catch (Exception e) {
            log(context, "BACKUP", "FAILED", null, e.getMessage());
            AppLogger.e("Backup failed", e);
            return false;
        }
    }

    public static boolean backupToUri(Context context, Uri target) {
        File tmp = new File(context.getCacheDir(), "tmp_backup_" + System.currentTimeMillis() + ".db");
        try {
            boolean ok = commitDbToCleanFile(context, tmp);
            if (!ok) return false;
            try (OutputStream os = context.getContentResolver().openOutputStream(target, "w")) {
                if (os == null) return false;
                copy(tmp, os);
            }
            log(context, "BACKUP", "OK", target.toString(), "Created backup");
            return true;
        } catch (Exception e) {
            log(context, "BACKUP", "FAILED", null, e.getMessage());
            AppLogger.e("Backup to URI failed", e);
            return false;
        } finally {
            if (tmp.exists()) tmp.delete();
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

    /**
     * Fully commits (checkpoints) the live database into a single, self-contained {@code -wal} /
     * {@code -shm}-free backup file written to {@code target}.
     *
     * <p>Produces the backup with {@code VACUUM INTO}, which writes a complete, consistent, standalone
     * SQLite file that includes every committed transaction, regardless of the live WAL state. This
     * avoids the fragile WAL-truncation checks and is safe even while Room keeps the database open and
     * other connections are attached. Nothing needs to be checkpointed or closed.</p>
     *
     * @return {@code true} if a validated, standalone backup file was written to {@code target}.
     */
    public static boolean commitDbToCleanFile(Context context, File target) {
        try {
            File parent = target.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            if (target.exists()) target.delete();

            AppDatabase appDb = AppDatabase.getInstance(context);
            appDb.getOpenHelper().getWritableDatabase()
                    .execSQL("VACUUM INTO '" + target.getPath().replace("'", "''") + "'");

            if (!target.exists() || target.length() == 0) {
                AppLogger.e("VACUUM INTO produced no file; aborting backup");
                return false;
            }

            try (android.database.sqlite.SQLiteDatabase check =
                         android.database.sqlite.SQLiteDatabase.openDatabase(
                                 target.getPath(), null,
                                 android.database.sqlite.SQLiteDatabase.OPEN_READONLY)) {
                android.database.Cursor c = check.rawQuery("PRAGMA integrity_check(1)", null);
                try {
                    if (c == null || !c.moveToFirst() || !"ok".equalsIgnoreCase(c.getString(0))) {
                        AppLogger.e("Backup integrity check failed");
                        return false;
                    }
                } finally {
                    if (c != null) c.close();
                }
            }
            return true;
        } catch (Exception e) {
            AppLogger.e("commitDbToCleanFile failed", e);
            return false;
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
