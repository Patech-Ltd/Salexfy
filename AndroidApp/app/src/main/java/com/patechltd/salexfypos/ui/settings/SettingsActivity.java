package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.util.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private static final long[] TIMEOUT_OPTIONS = {0, 30_000, 60_000, 2 * 60_000, 5 * 60_000, 10 * 60_000, 30 * 60_000};
    private static final String[] TIMEOUT_LABELS = {"Immediately", "30 seconds", "1 minute", "2 minutes", "5 minutes", "10 minutes", "30 minutes"};

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        findViewById(R.id.row_business).setOnClickListener(
                v -> startActivity(new Intent(this, BusinessSettingsActivity.class)));
        findViewById(R.id.row_payment_methods).setOnClickListener(
                v -> startActivity(new Intent(this, PaymentMethodsActivity.class)));
        findViewById(R.id.row_scanner).setOnClickListener(
                v -> startActivity(new Intent(this, ScannerSettingsActivity.class)));
        findViewById(R.id.row_printer).setOnClickListener(
                v -> startActivity(new Intent(this, PrinterSettingsActivity.class)));
        findViewById(R.id.row_loyalty).setOnClickListener(
                v -> startActivity(new Intent(this, LoyaltySettingsActivity.class)));
        findViewById(R.id.row_backup).setOnClickListener(
                v -> startActivity(new Intent(this, BackupSettingsActivity.class)));
        findViewById(R.id.row_sync).setOnClickListener(
                v -> startActivity(new Intent(this, SyncSettingsActivity.class)));
        findViewById(R.id.row_debug).setOnClickListener(
                v -> startActivity(new Intent(this, DebugActivity.class)));

        if (getIntent().getBooleanExtra("openBackup", false)) {
            startActivity(new Intent(this, BackupSettingsActivity.class));
        }

        SwitchMaterial appLockSwitch = findViewById(R.id.switch_app_lock);
        appLockSwitch.setChecked(Prefs.getBoolean(this, Prefs.KEY_APP_LOCK, false));
        appLockSwitch.setOnCheckedChangeListener((btn, checked) ->
                Prefs.putBoolean(this, Prefs.KEY_APP_LOCK, checked));

        updateTimeoutLabel();
        findViewById(R.id.row_lock_timeout).setOnClickListener(v -> showTimeoutPicker());
    }

    private void updateTimeoutLabel() {
        long current = Prefs.getLong(this, Prefs.KEY_APP_LOCK_TIMEOUT_MS, 2 * 60_000);
        String label = "2 minutes";
        for (int i = 0; i < TIMEOUT_OPTIONS.length; i++) {
            if (TIMEOUT_OPTIONS[i] == current) {
                label = TIMEOUT_LABELS[i];
                break;
            }
        }
        ((android.widget.TextView) findViewById(R.id.lock_timeout_value)).setText(label);
    }

    private void showTimeoutPicker() {
        long current = Prefs.getLong(this, Prefs.KEY_APP_LOCK_TIMEOUT_MS, 2 * 60_000);
        int selected = 3;
        for (int i = 0; i < TIMEOUT_OPTIONS.length; i++) {
            if (TIMEOUT_OPTIONS[i] == current) {
                selected = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Lock timeout")
                .setSingleChoiceItems(TIMEOUT_LABELS, selected, (dialog, which) -> {
                    Prefs.putLong(this, Prefs.KEY_APP_LOCK_TIMEOUT_MS, TIMEOUT_OPTIONS[which]);
                    updateTimeoutLabel();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
