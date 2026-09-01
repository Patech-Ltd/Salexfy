package com.patechltd.salexfypos.license;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.ui.settings.SyncSettingsActivity;
import com.patechltd.salexfypos.util.Prefs;

/**
 * Shown when the backend reports the license as invalid/expired. Allows the
 * user to either retry (re-validate) or go set up a license key in sync
 * settings. Offline/fail-open behaviour lets the POS keep running when the
 * server is simply unreachable.
 */
public class LicenseGateActivity extends AppCompatActivity {

    private static final long RETRY_MS = 30_000;

    private TextView messageText;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean stopping;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_license_gate);

        messageText = findViewById(R.id.license_message);

        findViewById(R.id.btn_retry).setOnClickListener(v -> revalidate());
        findViewById(R.id.btn_settings).setOnClickListener(v -> {
            startActivity(new Intent(this, SyncSettingsActivity.class));
        });
        findViewById(R.id.btn_continue_offline).setOnClickListener(v -> continueOffline());

        String message = getIntent().getStringExtra("message");
        boolean expired = getIntent().getBooleanExtra("expired", false);
        long expiresAt = getIntent().getLongExtra("expiresAt", 0);
        String shop = Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop");

        String text = expired
                ? "Your " + shop + " license has expired" + (expiresAt > 0
                    ? " on " + com.patechltd.salexfypos.util.DateUtil.formatDate(expiresAt) : "")
                : (message == null || message.isEmpty() ? "License validation failed" : message);
        messageText.setText(text);

        handler.postDelayed(revalidateRunnable, RETRY_MS);
    }

    private final Runnable revalidateRunnable = new Runnable() {
        @Override
        public void run() {
            if (!stopping && !isFinishing()) revalidate();
        }
    };

    private void revalidate() {
        new Thread(() -> {
            LicenseManager.Result r = LicenseManager.validate(this);
            if (r.valid) {
                runOnUiThread(this::continueOffline);
            } else {
                runOnUiThread(() -> {
                    if (!isFinishing()) {
                        messageText.setText(r.expired
                                ? "License still expired" : (r.message + " — retrying automatically"));
                        handler.postDelayed(revalidateRunnable, RETRY_MS);
                    }
                });
            }
        }).start();
    }

    private void continueOffline() {
        stopping = true;
        handler.removeCallbacks(revalidateRunnable);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
