package com.patechltd.salexfypos;

import android.app.Application;
import android.content.Context;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.patechltd.salexfypos.backup.BackupWorker;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.sync.SyncWorker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.CrashHandler;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.SoundUtil;

import java.util.concurrent.TimeUnit;

public class SalexfyApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        Context context = getApplicationContext();
        AppLogger.init(context);
        AppLogger.i("Salexfy POS starting");
        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler(this));
        SoundUtil.init(context);
        seedIfNeeded(context);
        scheduleAutoBackup(context);
        scheduleSync(context);
    }

    private void seedIfNeeded(Context context) {
        if (Prefs.getBoolean(context, Prefs.KEY_SEEDED, false)) return;
        Repository.get(context).run(() -> {
            try {
                SeedData.seed(context);
                Prefs.putBoolean(context, Prefs.KEY_SEEDED, true);
                AppLogger.i("Database seeded");
            } catch (Exception e) {
                AppLogger.e("Seeding failed", e);
            }
        });
    }

    private void scheduleAutoBackup(Context context) {
        int hours = Math.max(1, Prefs.getInt(context, Prefs.KEY_BACKUP_INTERVAL_HOURS, 6));
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(BackupWorker.class,
                hours, TimeUnit.HOURS)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                BackupWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    private void scheduleSync(Context context) {
        long minutes = Math.max(15, Prefs.getLong(context, Prefs.KEY_SYNC_INTERVAL_MINUTES, 30));
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class,
                minutes, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SyncWorker.UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    @Override
    public void onTerminate() {
        SoundUtil.release();
        super.onTerminate();
    }
}
