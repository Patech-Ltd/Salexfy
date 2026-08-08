package com.patechltd.salexfypos.scanner;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import java.util.concurrent.atomic.AtomicBoolean;

public class BarcodeAnalyzer implements ImageAnalysis.Analyzer {

    public interface OnResultListener {
        void onDecoded(String text, String format);
    }

    private final OnResultListener listener;
    private final DecodeEngine engine;
    private final AtomicBoolean active = new AtomicBoolean(true);
    private final long lockoutMs;
    private final Object lock = new Object();
    private long lastDecodeAt = 0;
    private volatile String lastResult = null;

    public BarcodeAnalyzer(OnResultListener listener, long lockoutMs, DecodeEngine engine) {
        this.listener = listener;
        this.lockoutMs = lockoutMs;
        this.engine = engine;
    }

    public void setActive(boolean active) {
        this.active.set(active);
    }

    public boolean isActive() {
        return active.get();
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
        if (!active.get()) {
            image.close();
            return;
        }
        try {
            long now = System.currentTimeMillis();
            synchronized (lock) {
                if (now - lastDecodeAt < lockoutMs) {
                    image.close();
                    return;
                }
            }
            DecodeResult result = engine.decode(image);
            if (result != null && result.text != null && !result.text.isEmpty()) {
                String text = result.text;
                String format = result.format;
                synchronized (lock) {
                    lastDecodeAt = System.currentTimeMillis();
                    if (text.equals(lastResult)) {
                        image.close();
                        return;
                    }
                    lastResult = text;
                }
                if (listener != null) listener.onDecoded(text, format);
            }
        } catch (Throwable ignored) {
        } finally {
            image.close();
        }
    }

    public void resetLock() {
        synchronized (lock) {
            lastDecodeAt = 0;
            lastResult = null;
        }
    }
}
