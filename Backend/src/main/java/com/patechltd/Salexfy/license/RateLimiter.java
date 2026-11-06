package com.patechltd.Salexfy.license;

import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

/** Small in-memory sliding-window limiter to blunt key brute-forcing. */
@Component
public class RateLimiter {

    private final ConcurrentHashMap<String, ArrayDeque<Long>> hits = new ConcurrentHashMap<>();

    /** @return true if the call is allowed (and is counted). */
    public boolean allow(String bucket, int max, long windowMs) {
        long now = System.currentTimeMillis();
        ArrayDeque<Long> q = hits.computeIfAbsent(bucket, k -> new ArrayDeque<>());
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > windowMs) q.pollFirst();
            if (q.size() >= max) return false;
            q.addLast(now);
            return true;
        }
    }

    /** Periodic cleanup so the map cannot grow without bound. */
    public void sweep(long windowMs) {
        long now = System.currentTimeMillis();
        hits.entrySet().removeIf(e -> {
            synchronized (e.getValue()) {
                return e.getValue().isEmpty() || now - e.getValue().peekLast() > windowMs;
            }
        });
    }
}
