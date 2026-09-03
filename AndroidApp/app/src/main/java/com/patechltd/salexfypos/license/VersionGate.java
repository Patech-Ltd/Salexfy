package com.patechltd.salexfypos.license;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AlertDialog;

import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Blocks NEW installations, not usage. Installations that happened before the
 * cutoff keep working forever; an APK installed on/after the cutoff refuses to
 * run and shows an "update required" dialog.
 *
 * Android cannot truly prevent sideloading, so the check is done on first
 * launch: the OS's persistent install timestamp can't be changed without root,
 * which makes this reasonably tamper-resistant.
 */
public final class VersionGate {

    /** New installs are rejected from 2026-09-05 00:00 local (after Friday 2026-09-04). */
    private static final int CUTOFF_YEAR = 2026;
    private static final int CUTOFF_MONTH = 9;      // 1 = January
    private static final int CUTOFF_DAY = 7;

    private VersionGate() {
    }

    /** True when THIS installation is a post-cutoff (forbidden) install. */
    public static boolean isBlocked(Context context) {
        long installTime = installTimeMillis(context);
        if (installTime <= 0) return false;      // unknown → fail open
        return installTime >= cutoffMillis();
    }

    private static long installTimeMillis(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    public static long cutoffMillis() {
        return ZonedDateTime.of(CUTOFF_YEAR, CUTOFF_MONTH, CUTOFF_DAY, 0, 0, 0, 0,
                        ZoneId.systemDefault())
                .toInstant().toEpochMilli();
    }

    /** Shows the non-cancelable lockout dialog. Call from the UI thread. */
    public static void block(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Update Required")
                .setMessage("A new version of Salexfy POS has been created.\n\n"
                        + "Please update this version and install the new version "
                        + "to continue using the app.")
                .setCancelable(false)
                .setPositiveButton("Exit App", (dialog, which) -> {
                    dialog.dismiss();
                    activity.finishAffinity();
                    System.exit(0);
                })
                .show();
    }
}