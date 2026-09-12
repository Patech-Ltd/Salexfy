package com.patechltd.salexfypos.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.TimeUnit;

public class BackupWorker extends Worker {

    public static final String UNIQUE_NAME = "salexfy_auto_backup";

    public BackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * (Re)schedules the periodic auto-backup with the current interval from prefs. Safe to call
     * after the user changes the frequency; the running schedule is replaced.
     */
    public static void schedule(Context context) {
        int hours = Math.max(1, Prefs.getInt(context, Prefs.KEY_BACKUP_INTERVAL_HOURS, 6));
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(BackupWorker.class,
                hours, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context ctx = getApplicationContext();
        if (!Prefs.getBoolean(ctx, Prefs.KEY_BACKUP_ENABLED, true)) {
            return Result.success();
        }
        try {
            File target = BackupManager.timestampedBackupFile(ctx);
            boolean ok = BackupManager.backupToFile(ctx, target);
            if (ok) {
                File dir = BackupManager.defaultBackupDir(ctx);
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
