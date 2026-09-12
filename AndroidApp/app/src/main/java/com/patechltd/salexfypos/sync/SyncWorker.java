package com.patechltd.salexfypos.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import java.util.concurrent.TimeUnit;

public class SyncWorker extends Worker {

    public static final String UNIQUE_NAME = "salexfy_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /**
     * (Re)schedules the periodic sync with the current interval from prefs. Only runs when the
     * device has network. Safe to call after the user changes the frequency.
     */
    public static void schedule(Context context) {
        long minutes = Math.max(15, Prefs.getLong(context, Prefs.KEY_SYNC_INTERVAL_MINUTES, 30));
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(SyncWorker.class,
                minutes, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 5, TimeUnit.MINUTES)
                .build();
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                UNIQUE_NAME, ExistingPeriodicWorkPolicy.UPDATE, request);
    }

    @NonNull
    @Override
    public Result doWork() {
        Context context = getApplicationContext();
        if (!Prefs.getBoolean(context, Prefs.KEY_SYNC_ENABLED, false)) {
            return Result.success();
        }
        String url = Prefs.getString(context, Prefs.KEY_SYNC_SERVER_URL, "");
        if (url.isEmpty()) {
            return Result.success();
        }
        try {
            boolean ok = SyncManager.syncNow(context);
            return ok ? Result.success() : Result.retry();
        } catch (Exception e) {
            AppLogger.e("Sync worker failed", e);
            return Result.retry();
        }
    }
}
