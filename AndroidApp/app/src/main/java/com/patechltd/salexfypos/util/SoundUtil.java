package com.patechltd.salexfypos.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.ToneGenerator;

public class SoundUtil {

    private static ToneGenerator toneGenerator;
    private static Context appContext;

    private SoundUtil() {
    }

    public static synchronized void init(Context context) {
        if (context != null) appContext = context.getApplicationContext();
        if (toneGenerator == null) {
            try {
                toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            } catch (Exception ignored) {
            }
        }
    }

    private static synchronized void ensure() {
        if (toneGenerator == null) init(appContext);
    }

    public static void beep() {
        if (appContext != null && !Prefs.getBoolean(appContext, Prefs.KEY_BEEP, true)) return;
        ensure();
        try {
            if (toneGenerator != null) toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 90);
        } catch (Exception ignored) {
        }
    }

    public static void errorBeep() {
        ensure();
        try {
            if (toneGenerator != null) toneGenerator.startTone(ToneGenerator.TONE_PROP_NACK, 120);
        } catch (Exception ignored) {
        }
    }

    public static synchronized void release() {
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
