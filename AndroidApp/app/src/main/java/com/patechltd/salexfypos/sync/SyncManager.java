package com.patechltd.salexfypos.sync;

import android.content.Context;

import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.SyncChange;
import com.patechltd.salexfypos.db.entity.SyncLog;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.Prefs;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SyncManager {

    private static final int PUSH_BATCH = 200;

    private SyncManager() {
    }

    public static boolean syncNow(Context context) {
        Repository repo = Repository.get(context);
        String base = serverBase(context);
        if (base.isEmpty()) return false;
        try {
            String token = login(context, base);
            if (token == null) {
                log(context, "SYNC", "FAILED", null, "Login failed");
                return false;
            }
            boolean ok = push(context, repo, base, token);
            ok = pull(context, repo, base, token) && ok;
            if (ok) {
                Prefs.putLong(context, Prefs.KEY_SYNC_LAST_SYNC, System.currentTimeMillis());
                SyncEvents.notifyDataChanged();
            }
            return ok;
        } catch (Exception e) {
            log(context, "SYNC", "FAILED", null, e.getMessage());
            AppLogger.e("Sync failed", e);
            return false;
        }
    }

    public static String testConnection(Context context) {
        String base = serverBase(context);
        if (base.isEmpty()) return "Enter the server URL first";
        try {
            String token = login(context, base);
            if (token == null) return "Login failed - check username and password";
            http("GET", base + "/api/sync/pull?since=0&deviceId=" + deviceId(context), null, token);
            return "Connected successfully";
        } catch (Exception e) {
            return "Connection failed: " + e.getMessage();
        }
    }

    public static int unsyncedCount(Context context) {
        try {
            return Repository.get(context).sync.countUnsynced();
        } catch (Throwable t) {
            return 0;
        }
    }

    private static String serverBase(Context context) {
        String base = Prefs.getString(context, Prefs.KEY_SYNC_SERVER_URL, "").trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    private static String login(Context context, String base) throws Exception {
        String username = Prefs.getString(context, Prefs.KEY_SYNC_USERNAME, "");
        String password = Prefs.getString(context, Prefs.KEY_SYNC_PASSWORD, "");
        JSONObject body = new JSONObject();
        body.put("username", username);
        body.put("password", password);
        String response = http("POST", base + "/api/auth/login", body.toString(), null);
        JSONObject obj = new JSONObject(response);
        String token = obj.optString("token");
        if (token.isEmpty()) token = obj.optString("accessToken");
        if (token.isEmpty() && obj.has("data")) {
            token = obj.optJSONObject("data").optString("token");
        }
        return token.isEmpty() ? null : token;
    }

    private static boolean push(Context context, Repository repo, String base, String token) {
        List<SyncChange> batch = repo.sync.getUnsynced(PUSH_BATCH);
        if (batch.isEmpty()) {
            log(context, "PUSH", "OK", "Nothing to send", null);
            return true;
        }
        try {
            JSONArray changes = new JSONArray();
            for (SyncChange c : batch) {
                JSONObject o = new JSONObject();
                o.put("seq", c.seq);
                o.put("entityType", c.entityType);
                o.put("recordId", c.recordId);
                o.put("operation", c.operation);
                o.put("payload", c.payload == null ? "" : c.payload);
                o.put("updatedAt", c.updatedAt);
                changes.put(o);
            }
            JSONObject body = new JSONObject();
            body.put("deviceId", deviceId(context));
            body.put("changes", changes);

            String response = http("POST", base + "/api/sync/push", body.toString(), token);
            JSONObject result = new JSONObject(response);
            JSONArray acked = result.optJSONArray("acknowledged");
            if (acked == null) {
                throw new IOException("Server response missing 'acknowledged'");
            }
            List<Long> ackedSeqs = new ArrayList<>();
            for (int i = 0; i < acked.length(); i++) {
                ackedSeqs.add(acked.getLong(i));
            }
            if (!ackedSeqs.isEmpty()) {
                repo.sync.markSynced(ackedSeqs, System.currentTimeMillis());
            }
            repo.sync.pruneSynced(5000);
            log(context, "PUSH", "OK", "Sent " + ackedSeqs.size() + " of " + batch.size() + " changes", null);
            return true;
        } catch (Exception e) {
            log(context, "PUSH", "FAILED", null, e.getMessage());
            AppLogger.e("Push failed", e);
            return false;
        }
    }

    private static boolean pull(Context context, Repository repo, String base, String token) {
        long since = repo.sync.lastSuccessfulPull();
        try {
            String response = http("GET", base + "/api/sync/pull?since=" + since
                    + "&deviceId=" + deviceId(context), null, token);
            JSONObject result = new JSONObject(response);
            JSONArray changes = result.optJSONArray("changes");
            int applied = 0;
            if (changes != null && changes.length() > 0) {
                List<RemoteChange> list = new ArrayList<>();
                for (int i = 0; i < changes.length(); i++) {
                    JSONObject c = changes.getJSONObject(i);
                    RemoteChange rc = new RemoteChange();
                    rc.entityType = c.optString("entityType");
                    rc.recordId = c.optString("recordId");
                    rc.operation = c.optString("operation");
                    rc.payload = c.optString("payload");
                    rc.updatedAt = c.optLong("updatedAt", 0);
                    list.add(rc);
                }
                applied = SyncApplier.apply(repo, list);
            }
            log(context, "PULL", "OK", "Applied " + applied + " changes from server", null);
            return true;
        } catch (Exception e) {
            log(context, "PULL", "FAILED", null, e.getMessage());
            AppLogger.e("Pull failed", e);
            return false;
        }
    }

    private static synchronized String deviceId(Context context) {
        String id = Prefs.getString(context, Prefs.KEY_DEVICE_ID, null);
        if (id == null || id.isEmpty()) {
            id = "android-" + UUID.randomUUID().toString();
            Prefs.putString(context, Prefs.KEY_DEVICE_ID, id);
        }
        return id;
    }

    private static String http(String method, String url, String jsonBody, String token) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestProperty("Content-Type", "application/json");
        if (token != null && !token.isEmpty()) {
            conn.setRequestProperty("Authorization", "Bearer " + token);
        }
        if (jsonBody != null) {
            conn.setDoOutput(true);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
            }
        }
        int code = conn.getResponseCode();
        InputStream is = code >= 400 ? conn.getErrorStream() : conn.getInputStream();
        String response = "";
        if (is != null) {
            try (InputStream stream = is) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[16384];
                int n;
                while ((n = stream.read(buf)) != -1) bos.write(buf, 0, n);
                response = bos.toString("UTF-8");
            }
        }
        conn.disconnect();
        if (code < 200 || code >= 300) {
            throw new IOException("HTTP " + code + (response.isEmpty() ? "" : ": " + response));
        }
        return response;
    }

    private static void log(Context context, String type, String status, String message, String details) {
        try {
            SyncLog log = new SyncLog();
            log.uid = UUID.randomUUID().toString();
            log.timestamp = System.currentTimeMillis();
            log.type = type;
            log.status = status;
            log.message = message;
            log.details = details;
            Repository.get(context).sync.insertLog(log);
            Repository.get(context).sync.pruneLogs(System.currentTimeMillis() - 7L * 86400000L);
        } catch (Throwable ignored) {
        }
    }
}
