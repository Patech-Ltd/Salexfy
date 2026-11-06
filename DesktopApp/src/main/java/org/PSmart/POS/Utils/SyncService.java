package org.PSmart.POS.Utils;

import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Sync service: pushes a local backup snapshot to a configurable server via HTTP
 * (multipart-ish single-file POST with Basic auth). Keeps the last attempt result
 * in the StoreConfig so the UI can show a live sync status.
 */
public final class SyncService {

    private SyncService() {}

    public static final String STATUS_OK = "Sync complete";
    public static final String STATUS_SYNCING = "Syncing...";
    public static final String STATUS_FAIL = "Sync failed";
    public static final String STATUS_IDLE = "Not configured";

    /**
     * Push the given file to the server. Uses Basic auth from config.
     * Returns a human-readable result ("Sync complete KES...") or throws on failure.
     */
    public static String syncNow(StoreConfig config, File file) throws Exception {
        if (config == null || !Boolean.TRUE.equals(config.getSyncEnabled())) {
            throw new IllegalStateException("Sync is not enabled.");
        }
        String urlStr = config.getSyncServerUrl();
        if (urlStr == null || urlStr.trim().isEmpty()) {
            throw new IllegalStateException("No server URL configured.");
        }

        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/octet-stream");
        conn.setRequestProperty("X-File-Name", file.getName());

        String user = config.getSyncUsername() == null ? "" : config.getSyncUsername();
        String pass = config.getSyncPassword() == null ? "" : config.getSyncPassword();
        if (!user.isEmpty()) {
            String token = Base64.getEncoder().encodeToString(
                    (user + ":" + pass).getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + token);
        }

        try (FileInputStream in = new FileInputStream(file);
             DataOutputStream out = new DataOutputStream(conn.getOutputStream())) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
        }

        int code = conn.getResponseCode();
        String body = readBody(conn);
        if (code >= 200 && code < 300) {
            recordStatus(null, STATUS_OK, System.currentTimeMillis());
            return STATUS_OK + (body != null && !body.isEmpty() ? " (" + body + ")" : "");
        } else {
            recordStatus(null, STATUS_FAIL, System.currentTimeMillis());
            throw new IllegalStateException("Server responded " + code + ": " + body);
        }
    }

    private static String readBody(HttpURLConnection conn) {
        try {
            java.io.InputStream is = conn.getInputStream();
            byte[] buf = is.readAllBytes();
            return new String(buf, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return null;
        }
    }

    /** Persist the last sync status/time into the config row. */
    public synchronized static void recordStatus(Runnable before, String status, Long when) {
        try {
            StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
            if (cfg == null) cfg = StoreConfig.defaultConfig();
            cfg.setLastSyncStatus(status);
            cfg.setLastSyncAt(when);
            DatabaseUtils.getInstance().getStoreConfigRepository().saveOrUpdate(cfg);
            if (before != null) before.run();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}