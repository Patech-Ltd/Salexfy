package com.patechltd.salexfypos.sync;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class SyncEvents {

    private static final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    private SyncEvents() {
    }

    public static void addListener(Runnable listener) {
        listeners.add(listener);
    }

    public static void removeListener(Runnable listener) {
        listeners.remove(listener);
    }

    public static void notifyDataChanged() {
        for (Runnable listener : listeners) {
            try {
                listener.run();
            } catch (Throwable ignored) {
            }
        }
    }
}
