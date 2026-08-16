package com.patechltd.salexfypos.sync;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

public class SyncWorker extends Worker {

    public static final String UNIQUE_NAME = "salexfy_sync";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
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
