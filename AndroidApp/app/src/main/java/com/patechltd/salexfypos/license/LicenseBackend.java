package com.patechltd.salexfypos.license;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Base64;

import com.patechltd.salexfypos.sync.SyncManager;
import com.patechltd.salexfypos.util.Prefs;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

/**
 * Backend-enforced licensing.
 *
 * Device identity = generated device id + model + manufacturer + package install time
 * + hardware hash (board/hardware/ANDROID_ID). The server binds the license to that
 * identity and returns a signed token ("lease"). The app:
 *   - accepts a token only if the pinned public key verifies it AND it echoes our
 *     fresh nonce AND it names this exact device fingerprint (no replay, no transplant);
 *   - runs fully offline until the lease ends, then must phone home;
 *   - detects clock roll-back using a monotonic "latest time seen" watermark.
 */
public final class LicenseBackend {

    private static final long CLOCK_SKEW_MS = 10 * 60 * 1000L;

    private LicenseBackend() {
    }

    public static boolean isBackendMode(Context context) {
        return LicenseConfig.MODE == LicenseConfig.Mode.BACKEND;
    }

    static LicenseManager.Result validate(Context context) {
        if (!LicenseCrypto.isConfigured()) {
            return new LicenseManager.Result(false, false,
                    "Backend licensing is not configured in this build (public key missing)", 0);
        }
        String key = Prefs.getString(context, Prefs.KEY_LICENSE_KEY, "").trim();
        if (key.isEmpty()) {
            return new LicenseManager.Result(false, false, "Enter your license key in Settings", 0);
        }
        String base = ServerUtil.serverBase(context);
        if (base.isEmpty()) {
            return new LicenseManager.Result(false, false, "Set the server URL in Settings", 0);
        }

        String nonce = newNonce();
        JSONObject resp;
        try {
            boolean hasToken = !Prefs.getString(context, Prefs.KEY_LICENSE_TOKEN, "").isEmpty();
            resp = call(context, base, hasToken ? "validate" : "activate", key, nonce);
            if (hasToken && "NOT_ACTIVATED".equals(resp.optString("code"))) {
                resp = call(context, base, "activate", key, nonce);
            }
        } catch (Exception e) {
            return offlineLease(context);
        }

        if (!resp.optBoolean("ok", false)) {
            String code = resp.optString("code", "INVALID");
            if ("RATE_LIMITED".equals(code) || "BAD_REQUEST".equals(code)) {
                return offlineLease(context);   // not a licence verdict
            }
            Prefs.putString(context, Prefs.KEY_LICENSE_TOKEN, "");
            return new LicenseManager.Result(false, "EXPIRED".equals(code),
                    resp.optString("message", "License rejected"), resp.optLong("expiresAt", 0));
        }

        JSONObject payload = LicenseCrypto.verify(resp.optString("token", null));
        String problem = checkPayload(context, payload, nonce, true);
        if (problem != null) {
            return new LicenseManager.Result(false, false, problem, 0);
        }
        Prefs.putString(context, Prefs.KEY_LICENSE_TOKEN, resp.optString("token"));
        Prefs.putLong(context, Prefs.KEY_LICENSE_EXPIRY, payload.optLong("exp"));
        touchWatermark(context, resp.optLong("serverTime", 0));
        return new LicenseManager.Result(true, false, "License valid", payload.optLong("exp"));
    }

    /** Offline path: trust only a previously stored, signature-valid, unexpired lease. */
    private static LicenseManager.Result offlineLease(Context context) {
        String token = Prefs.getString(context, Prefs.KEY_LICENSE_TOKEN, "");
        if (token.isEmpty()) {
            return new LicenseManager.Result(false, false,
                    "Cannot reach the license server. Connect to the internet to activate.", 0);
        }
        JSONObject payload = LicenseCrypto.verify(token);
        String problem = checkPayload(context, payload, null, false);
        if (problem != null) {
            return new LicenseManager.Result(false, false, problem, 0);
        }
        long now = System.currentTimeMillis();
        long hoursLeft = Math.max(0, (payload.optLong("lease") - now) / 3_600_000L);
        touchWatermark(context, 0);
        return new LicenseManager.Result(true, false,
                "Offline - must reconnect within " + hoursLeft + "h", payload.optLong("exp"));
    }

    /** @return null when the payload is acceptable, otherwise a user-facing reason. */
    private static String checkPayload(Context context, JSONObject p, String expectedNonce, boolean fresh) {
        if (p == null) return "License signature invalid";
        if (!deviceId(context).equals(p.optString("dev"))
                || !fingerprint(context).equals(p.optString("fp"))) {
            return "License belongs to a different device";
        }
        if (expectedNonce != null && !expectedNonce.equals(p.optString("nonce"))) {
            return "License response failed freshness check";
        }
        long now = System.currentTimeMillis();
        long watermark = Prefs.getLong(context, Prefs.KEY_LICENSE_WATERMARK, 0);
        if (watermark > 0 && now + CLOCK_SKEW_MS < watermark) {
            return "Device clock was set back. Correct the date/time and reconnect.";
        }
        if (now > p.optLong("exp")) return "License expired";
        if (!fresh && now > p.optLong("lease")) {
            return "License check overdue. Connect to the internet to continue.";
        }
        return null;
    }

    private static void touchWatermark(Context context, long serverTime) {
        long now = Math.max(System.currentTimeMillis(), serverTime);
        if (now > Prefs.getLong(context, Prefs.KEY_LICENSE_WATERMARK, 0)) {
            Prefs.putLong(context, Prefs.KEY_LICENSE_WATERMARK, now);
        }
    }

    private static JSONObject call(Context context, String base, String endpoint, String key,
                                   String nonce) throws Exception {
        JSONObject body = new JSONObject();
        body.put("licenseKey", key);
        body.put("deviceId", deviceId(context));
        body.put("model", Build.MODEL);
        body.put("manufacturer", Build.MANUFACTURER);
        body.put("installTime", installTime(context));
        body.put("hwHash", hwHash(context));
        body.put("appVersion", appVersion(context));
        body.put("nonce", nonce);
        try {
            return new JSONObject(ServerUtil.http("POST", base + "/api/license/v2/" + endpoint,
                    body.toString(), null));
        } catch (java.io.IOException e) {
            // Denials arrive as HTTP 200 JSON; a 4xx/5xx body (e.g. 400) is also JSON we can read.
            String m = e.getMessage();
            int i = m == null ? -1 : m.indexOf('{');
            if (i >= 0) {
                try {
                    return new JSONObject(m.substring(i));
                } catch (Exception ignored) {
                    // fall through
                }
            }
            throw e;
        }
    }

    // ---- identity ------------------------------------------------------------

    static String deviceId(Context context) {
        return SyncManager.deviceId(context);
    }

    static long installTime(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            return 1;
        }
    }

    private static String appVersion(Context context) {
        try {
            String v = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
            return v == null ? "" : v;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    static String hwHash(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        String raw = Build.BOARD + "|" + Build.HARDWARE + "|" + Build.BRAND + "|" + Build.DEVICE
                + "|" + (androidId == null ? "" : androidId);
        return b64(sha256(raw));
    }

    /** Must mirror the server: sha256(deviceId|model|manufacturer|installTime|hwHash) as hex. */
    static String fingerprint(Context context) {
        String raw = deviceId(context) + "|" + Build.MODEL + "|" + Build.MANUFACTURER
                + "|" + installTime(context) + "|" + hwHash(context);
        StringBuilder sb = new StringBuilder();
        for (byte b : sha256(raw)) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private static byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String b64(byte[] b) {
        return Base64.encodeToString(b, Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP);
    }

    private static String newNonce() {
        byte[] b = new byte[18];
        new SecureRandom().nextBytes(b);
        return b64(b);
    }
}
