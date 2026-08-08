package com.patechltd.salexfypos.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class BackupWorker extends Worker {

    public static final String UNIQUE_NAME = "salexfy_auto_backup";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            if (!Prefs.getBoolean(getApplicationContext(), Prefs.KEY_BACKUP_ENABLED, true)) {
                return Result.success();
            }
            File target = BackupManager.timestampedBackupFile(getApplicationContext());
            boolean ok = BackupManager.backupToFile(getApplicationContext(), target);
            if (ok) {
                File dir = BackupManager.defaultBackupDir(getApplicationContext());
                File[] files = dir.listFiles((d, name) -> name.startsWith("salexfy_backup_"));
                if (files != null && files.length > 10) {
                    Arrays.sort(files, Comparator.comparingLong(File::lastModified));
                    for (int i = 0; i < files.length - 10; i++) {
                        files[i].delete();
                    }
                }
            }
            return ok ? Result.success() : Result.retry();
        } catch (Exception e) {
            AppLogger.e("Auto backup worker failed", e);
            return Result.retry();
        }
    }
}
