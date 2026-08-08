package com.patechltd.salexfypos.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.widget.Toast;

import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.CrashLog;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.security.Session;

import java.util.UUID;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private final Context context;
    private final Thread.UncaughtExceptionHandler defaultHandler;

    public CrashHandler(Context context) {
        this.context = context.getApplicationContext();
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            saveCrash(thread, throwable, true);
        } catch (Throwable ignored) {
        }
        try {
            Toast.makeText(context, "Sorry, the app crashed. We have saved the error for review.", Toast.LENGTH_LONG).show();
        } catch (Throwable ignored) {
        }
        try {
            String summary = throwable != null && throwable.getMessage() != null
                    ? throwable.getMessage()
                    : String.valueOf(throwable);
            String line = thread != null ? thread.getName() : "unknown";
            Prefs.putString(context, Prefs.KEY_PENDING_CRASH,
                    "Crash on thread: " + line + "\n\n" + summary
                            + "\n\n" + android.util.Log.getStackTraceString(throwable));
        } catch (Throwable ignored) {
        }
        try {
            PendingIntent pi = PendingIntent.getActivity(context, 0,
                    new Intent(context, com.patechltd.salexfypos.ui.crash.CrashRecoveryActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK),
                    PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);
            AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            am.set(AlarmManager.RTC, System.currentTimeMillis() + 1200, pi);
        } catch (Throwable ignored) {
        }
        if (defaultHandler != null) {
            defaultHandler.uncaughtException(thread, throwable);
        }
    }

    public static void logCaught(Context context, String message, Throwable t) {
        try {
            saveCrashSync(context, Thread.currentThread(), t != null ? t : new RuntimeException(message), false);
        } catch (Throwable ignored) {
        }
    }

    private void saveCrash(Thread thread, Throwable throwable, boolean fatal) {
        Repository repo = Repository.get(context);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            repo.run(() -> {
                CrashLog log = build(context, thread, throwable, fatal);
                repo.crash.insertCrash(log);
            });
        } else {
            CrashLog log = build(context, thread, throwable, fatal);
            repo.crash.insertCrash(log);
        }
        AppLogger.e(fatal ? "FATAL CRASH" : "CAUGHT ERROR", throwable);
    }

    private static void saveCrashSync(Context context, Thread thread, Throwable throwable, boolean fatal) {
        Repository repo = Repository.get(context);
        if (Looper.myLooper() == Looper.getMainLooper()) {
            repo.run(() -> {
                CrashLog log = build(context, thread, throwable, fatal);
                repo.crash.insertCrash(log);
            });
        } else {
            CrashLog log = build(context, thread, throwable, fatal);
            repo.crash.insertCrash(log);
        }
        AppLogger.e("CAUGHT ERROR", throwable);
    }

    private static CrashLog build(Context context, Thread thread, Throwable throwable, boolean fatal) {
        CrashLog log = new CrashLog();
        log.id = UUID.randomUUID().toString();
        log.timestamp = System.currentTimeMillis();
        log.threadName = thread != null ? thread.getName() : "unknown";
        Throwable t = throwable != null ? throwable : new Throwable("unknown");
        log.message = String.valueOf(t.getMessage());
        log.stackTrace = android.util.Log.getStackTraceString(t);
        log.isFatal = fatal;
        log.appVersion = "1.0";
        try {
            String userId = Session.userId(context);
            if (userId != null) {
                User u = Repository.get(context).admin.getUser(userId);
                if (u != null) {
                    log.stackTrace = "[User: " + u.username + "]\n" + log.stackTrace;
                }
            }
        } catch (Throwable ignored) {
        }
        return log;
    }
}
