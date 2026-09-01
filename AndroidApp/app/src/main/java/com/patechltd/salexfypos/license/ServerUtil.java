package com.patechltd.salexfypos.license;

import android.content.Context;

import com.patechltd.salexfypos.sync.SyncManager;
import com.patechltd.salexfypos.util.Prefs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/** Shared, lightweight HTTP + config helpers for talking to the sync server. */
final class ServerUtil {

    private ServerUtil() {
    }

    static String serverBase(Context context) {
        String base = Prefs.getString(context, Prefs.KEY_SYNC_SERVER_URL, "").trim();
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        return base;
    }

    static String http(String method, String url, String jsonBody, String token) throws Exception {
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
}
