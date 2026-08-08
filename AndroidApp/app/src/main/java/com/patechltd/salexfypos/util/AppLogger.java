package com.patechltd.salexfypos.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.concurrent.locks.ReentrantLock;

public class AppLogger {

    private static final String TAG = "Salexfy";
    private static final int MAX_LINES = 2000;
    private static File logFile;
    private static final ReentrantLock LOCK = new ReentrantLock();

    private AppLogger() {
    }

    public static void init(Context context) {
        logFile = new File(context.getFilesDir(), "app_log.txt");
    }

    public static void d(String message) {
        write("DEBUG", message);
        Log.d(TAG, message);
    }

    public static void i(String message) {
        write("INFO", message);
        Log.i(TAG, message);
    }

    public static void e(String message, Throwable t) {
        write("ERROR", message + (t != null ? "\n" + Log.getStackTraceString(t) : ""));
        Log.e(TAG, message, t);
    }

    public static void e(String message) {
        write("ERROR", message);
        Log.e(TAG, message);
    }

    private static void write(String level, String message) {
        if (logFile == null) return;
        LOCK.lock();
        try {
            if (logFile.length() > 512 * 1024) {
                logFile.delete();
            }
            try (PrintWriter out = new PrintWriter(new FileWriter(logFile, true))) {
                String ts = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
                        .format(new java.util.Date());
                out.println(ts + " [" + level + "] " + message);
                trim();
            }
        } catch (Exception ignored) {
        } finally {
            LOCK.unlock();
        }
    }

    private static void trim() {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            java.util.List<String> lines = new java.util.ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) lines.add(line);
            reader.close();
            if (lines.size() > MAX_LINES) {
                java.util.List<String> tail = lines.subList(lines.size() - MAX_LINES, lines.size());
                try (PrintWriter out = new PrintWriter(new FileWriter(logFile, false))) {
                    for (String s : tail) out.println(s);
                }
            }
        } catch (Exception ignored) {
        }
    }

    public static String readLog() {
        if (logFile == null || !logFile.exists()) return "(no logs yet)";
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(logFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
                if (sb.length() > 200_000) break;
            }
        } catch (Exception ignored) {
        }
        return sb.length() == 0 ? "(no logs yet)" : sb.toString();
    }

    public static void clear() {
        if (logFile != null) logFile.delete();
    }
}
