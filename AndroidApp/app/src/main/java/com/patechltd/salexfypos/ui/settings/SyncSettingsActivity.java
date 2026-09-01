package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.sync.SyncManager;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;

public class SyncSettingsActivity extends AppCompatActivity {

    private SwitchMaterial syncSwitch;
    private TextInputEditText serverUrl;
    private TextInputEditText username;
    private TextInputEditText password;
    private TextInputEditText interval;
    private TextInputEditText licenseKey;
    private TextView lastSync;
    private TextView unsyncedText;
    private MaterialButton btnSyncNow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        syncSwitch = findViewById(R.id.switch_sync);
        serverUrl = findViewById(R.id.input_server_url);
        username = findViewById(R.id.input_username);
        password = findViewById(R.id.input_password);
        interval = findViewById(R.id.input_interval);
        licenseKey = findViewById(R.id.input_license_key);
        lastSync = findViewById(R.id.last_sync);
        unsyncedText = findViewById(R.id.unsynced_text);
        btnSyncNow = findViewById(R.id.btn_sync_now);

        load();

        ((MaterialButton) findViewById(R.id.btn_test)).setOnClickListener(v -> testConnection());
        btnSyncNow.setOnClickListener(v -> syncNow());
        ((MaterialButton) findViewById(R.id.btn_view_logs)).setOnClickListener(v ->
                startActivity(new Intent(this, SyncLogActivity.class)));
        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void load() {
        syncSwitch.setChecked(Prefs.getBoolean(this, Prefs.KEY_SYNC_ENABLED, false));
        serverUrl.setText(Prefs.getString(this, Prefs.KEY_SYNC_SERVER_URL, ""));
        username.setText(Prefs.getString(this, Prefs.KEY_SYNC_USERNAME, ""));
        password.setText(Prefs.getString(this, Prefs.KEY_SYNC_PASSWORD, ""));
        interval.setText(String.valueOf(Prefs.getLong(this, Prefs.KEY_SYNC_INTERVAL_MINUTES, 30)));
        licenseKey.setText(Prefs.getString(this, Prefs.KEY_LICENSE_KEY, ""));
        updateStatus();
    }

    private void updateStatus() {
        long last = Prefs.getLong(this, Prefs.KEY_SYNC_LAST_SYNC, 0);
        lastSync.setText("Last sync: " + (last == 0
                ? "Never" : DateUtil.formatDate(last) + " " + DateUtil.formatTime(last)));
        updateUnsynced();
    }

    private void updateUnsynced() {
        final int count = SyncManager.unsyncedCount(this);
        unsyncedText.setText(count + " unsynced change" + (count == 1 ? "" : "s"));
    }

    private void save() {
        Prefs.putBoolean(this, Prefs.KEY_SYNC_ENABLED, syncSwitch.isChecked());
        Prefs.putString(this, Prefs.KEY_SYNC_SERVER_URL,
                textOf(serverUrl));
        Prefs.putString(this, Prefs.KEY_SYNC_USERNAME, textOf(username));
        Prefs.putString(this, Prefs.KEY_SYNC_PASSWORD, textOf(password));
        Prefs.putString(this, Prefs.KEY_LICENSE_KEY, textOf(licenseKey));
        long minutes = parseLong(interval);
        Prefs.putLong(this, Prefs.KEY_SYNC_INTERVAL_MINUTES, Math.max(15, minutes));
        DialogUtil.toast(this, "Settings saved");
        finish();
    }

    private void testConnection() {
        saveToPrefsSilently();
        final SyncSettingsActivity self = this;
        final MaterialButton btn = findViewById(R.id.btn_test);
        btn.setEnabled(false);
        new Thread(() -> {
            String result = SyncManager.testConnection(self);
            runOnUiThread(() -> {
                btn.setEnabled(true);
                DialogUtil.toast(self, result);
            });
        }).start();
    }

    private void syncNow() {
        saveToPrefsSilently();
        final SyncSettingsActivity self = this;
        btnSyncNow.setEnabled(false);
        new Thread(() -> {
            final boolean ok = SyncManager.syncNow(self);
            runOnUiThread(() -> {
                btnSyncNow.setEnabled(true);
                updateStatus();
                DialogUtil.toast(self, ok ? "Sync completed" : "Sync failed - see logs");
            });
        }).start();
    }

    private void saveToPrefsSilently() {
        Prefs.putBoolean(this, Prefs.KEY_SYNC_ENABLED, syncSwitch.isChecked());
        Prefs.putString(this, Prefs.KEY_SYNC_SERVER_URL, textOf(serverUrl));
        Prefs.putString(this, Prefs.KEY_SYNC_USERNAME, textOf(username));
        Prefs.putString(this, Prefs.KEY_SYNC_PASSWORD, textOf(password));
        Prefs.putString(this, Prefs.KEY_LICENSE_KEY, textOf(licenseKey));
        long minutes = parseLong(interval);
        Prefs.putLong(this, Prefs.KEY_SYNC_INTERVAL_MINUTES, Math.max(15, minutes));
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    private long parseLong(TextInputEditText input) {
        try {
            return Long.parseLong(textOf(input));
        } catch (NumberFormatException e) {
            return 30;
        }
    }
}
