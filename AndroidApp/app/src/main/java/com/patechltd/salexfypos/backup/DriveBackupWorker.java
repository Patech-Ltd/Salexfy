package com.patechltd.salexfypos.backup;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.patechltd.salexfypos.drive.DriveBackupManager;
import com.patechltd.salexfypos.drive.DriveServiceHelper;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import java.util.concurrent.TimeUnit;

/**
 * Uploads the database to Google Drive on its own schedule, independent of the
 * local internal-storage backup ({@link BackupWorker}). Each worker has its own
 * periodic plan so local and Drive backups can use different frequencies.
 */
public class DriveBackupWorker extends Worker {

    public static final String UNIQUE_NAME = "salexfy_drive_upload";

    public DriveBackupWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    /** (Re)schedules the periodic Drive upload with the current interval from prefs. */
    public static void schedule(Context context) {
        long hours = Math.max(1,
                Prefs.getLong(context, Prefs.KEY_BACKUP_DRIVE_INTERVAL_HOURS, 12));
        PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(DriveBackupWorker.class,
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
        if (!Prefs.getBoolean(ctx, Prefs.KEY_BACKUP_DRIVE_ENABLED, false)
                || Prefs.getString(ctx, Prefs.KEY_BACKUP_DRIVE_EMAIL, null) == null) {
            return Result.success();
        }
        try {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(ctx);
            if (account == null || !DriveServiceHelper.hasDriveScope(account)) {
                AppLogger.i("Auto Drive upload skipped: no valid Drive session");
                return Result.success();
            }
            boolean ok = DriveBackupManager.backup(ctx,
                    DriveServiceHelper.getDrive(ctx, account));
            return ok ? Result.success() : Result.retry();
        } catch (Exception e) {
            AppLogger.e("Auto Drive upload failed", e);
            return Result.retry();
        }
    }
}