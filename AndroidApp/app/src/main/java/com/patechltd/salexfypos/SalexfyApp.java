package com.patechltd.salexfypos;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.patechltd.salexfypos.backup.BackupWorker;
import com.patechltd.salexfypos.db.AppDatabase;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.license.LicenseManager;
import com.patechltd.salexfypos.sync.SyncWorker;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.CrashHandler;
import com.patechltd.salexfypos.util.Notifier;
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
        Notifier.ensureChannels(context);
        AppDatabase.warmUp(context);
        seedIfNeeded(context);
        scheduleAutoBackup(context);
        scheduleSync(context);
        validateLicenseInBackground(context);

        applyThemeMode(context);


        registerActivityLifecycleCallbacks(
                new ActivityLifecycleCallbacks() {

                    @Override
                    public void onActivityCreated(
                            Activity activity,
                            Bundle savedInstanceState) {

                        View content = activity.findViewById(android.R.id.content);

                        ViewCompat.setOnApplyWindowInsetsListener(content, (v, insets) -> {

                            Insets bars = insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                            v.setPadding(
                                    bars.left,
                                    bars.top,
                                    bars.right,
                                    bars.bottom
                            );

                            return insets;
                        });

                        ViewCompat.requestApplyInsets(content);
                    }

                    // other methods can remain empty
                    @Override public void onActivityStarted(Activity a) {}
                    @Override public void onActivityResumed(Activity a) {}
                    @Override public void onActivityPaused(Activity a) {}
                    @Override public void onActivityStopped(Activity a) {}
                    @Override public void onActivitySaveInstanceState(Activity a, Bundle b) {}
                    @Override public void onActivityDestroyed(Activity a) {}
                }
        );
    }

    /**
     * Kicks off a background license validation. Never blocks startup. If the
     * license is found invalid/expired from a responsive server, we record that
     * in prefs; the gate itself is enforced in MainActivity so the user is not
     * locked mid-launch.
     */
    private void validateLicenseInBackground(Context context) {
        new Thread(() -> {
            try {
                LicenseManager.validate(context);
            } catch (Throwable t) {
                AppLogger.d("License background check failed: " + t.getMessage());
            }
        }).start();
    }

    /**
     * Applies the user-selected theme mode (system / light / dark) stored in prefs.
     */
    private void applyThemeMode(Context context) {
        int nightMode;
        switch (Prefs.getString(context, Prefs.KEY_THEME_MODE, "system")) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
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
