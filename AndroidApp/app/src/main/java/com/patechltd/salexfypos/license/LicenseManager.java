package com.patechltd.salexfypos.license;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import com.patechltd.salexfypos.sync.SyncManager;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import org.json.JSONObject;

import java.security.MessageDigest;

/**
 * Validates the device's license against the backend when sync is reachable.
 *
 * Behaviour:
 *  - If sync is disabled OR the server is unreachable, the app continues to run
 *    offline (fail-open) within a grace period.
 *  - If the server DOES respond and the license is invalid/expired, the app is
 *    locked (fail-closed) so a lapsed license can't keep using the POS.
 *  - Validated expiry is cached so the app can't be fooled by simply turning
 *    off the network to hide an expired license forever; after the cached expiry
 *    passes, offline usage is blocked.
 */
public final class LicenseManager {

    /** Offline grace beyond the cached expiry before we start blocking. */
    private static final long OFFLINE_GRACE_MS = 72L * 60 * 60 * 1000;

    private LicenseManager() {
    }

    public static final class Result {
        public final boolean valid;
        public final boolean expired;
        public final String message;
        public final long expiresAt;

        Result(boolean valid, boolean expired, String message, long expiresAt) {
            this.valid = valid;
            this.expired = expired;
            this.message = message;
            this.expiresAt = expiresAt;
        }
    }

    public static Result validate(Context context) {
        String licenseKey = Prefs.getString(context, Prefs.KEY_LICENSE_KEY, "");
        if (licenseKey == null || licenseKey.trim().isEmpty()) {
            // No license configured yet — offline installs run in trial/self-managed mode.
            return new Result(true, false, "No license configured (offline mode)", 0);
        }

        String base = ServerUtil.serverBase(context);
        if (base.isEmpty()) {
            return offlineCheck(context);
        }

        try {
            JSONObject body = new JSONObject();
            body.put("deviceId", SyncManager.deviceId(context));
            body.put("shopId", Prefs.getString(context, Prefs.KEY_SHOP_ID, "default"));
            body.put("licenseKey", licenseKey.trim());

            String response = ServerUtil.http("POST", base + "/api/license/validate",
                    body.toString(), null);
            JSONObject result = new JSONObject(response);
            boolean valid = result.optBoolean("valid", false);
            boolean expired = result.optBoolean("expired", false);
            long expiresAt = result.optLong("expiresAt", 0);
            String message = result.optString("message", "");

            if (expiresAt > 0) {
                Prefs.putLong(context, Prefs.KEY_LICENSE_EXPIRY, expiresAt);
            }

            if (valid) {
                AppLogger.i("License valid until " + expiresAt);
                Prefs.putLong(context, Prefs.KEY_LICENSE_EXPIRY, expiresAt);
                return new Result(true, false, message, expiresAt);
            }
            if (expired) {
                return new Result(false, true, message, expiresAt);
            }
            return new Result(false, false, message, expiresAt);
        } catch (Exception e) {
            // Server reachable but errored → use offline fallback with cached expiry.
            AppLogger.d("License validation failed, falling back offline: " + e.getMessage());
            return offlineCheck(context);
        }
    }

    /**
     * Offline check: if we have a cached expiry and it has now passed (plus a
     * grace period), block. Otherwise continue running.
     */
    private static Result offlineCheck(Context context) {
        long expiry = Prefs.getLong(context, Prefs.KEY_LICENSE_EXPIRY, 0);
        if (expiry == 0) {
            return new Result(true, false, "Offline license check pending", 0);
        }
        long graceEnd = expiry + OFFLINE_GRACE_MS;
        long now = System.currentTimeMillis();
        if (now > graceEnd) {
            return new Result(false, true, "License expired and offline grace used up", expiry);
        }
        long remaining = (graceEnd - now);
        return new Result(true, false,
                "Offline mode — grace remaining " + (remaining / (60 * 60 * 1000)) + "h", expiry);
    }

    /**
     * A stable, un-spoofable-ish device fingerprint. Not perfect but hard enough
     * to brute-force a per-device license.
     */
    public static String deviceFingerprint(Context context) {
        StringBuilder sb = new StringBuilder();
        sb.append(Build.MODEL).append('|')
                .append(Build.MANUFACTURER).append('|')
                .append(Build.BOARD).append('|')
                .append(Build.HARDWARE).append('|');
        try {
            String serial = (String) Build.class.getField("SERIAL").get(null);
            sb.append(serial);
        } catch (Exception ignored) {
            sb.append("unknown");
        }
        sb.append('|').append(Prefs.getString(context, Prefs.KEY_DEVICE_ID, ""));
        return sha1(sb.toString());
    }

    private static String sha1(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(input.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
