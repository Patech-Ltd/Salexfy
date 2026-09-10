package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.util.Prefs;

public class SettingsActivity extends AppCompatActivity {

    private static final long[] TIMEOUT_OPTIONS = {0, 30_000, 60_000, 2 * 60_000, 5 * 60_000, 10 * 60_000, 30 * 60_000};
    private static final String[] TIMEOUT_LABELS = {"Immediately", "30 seconds", "1 minute", "2 minutes", "5 minutes", "10 minutes", "30 minutes"};
    private static final String[] THEME_LABELS = {"Pick from system", "Light", "Dark"};
    private static final String[] THEME_VALUES = {"system", "light", "dark"};

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

        updateThemeSubtitle();
        findViewById(R.id.row_theme).setOnClickListener(v -> showThemePicker());
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

    private void updateThemeSubtitle() {
        String current = Prefs.getString(this, Prefs.KEY_THEME_MODE, "system");
        String label = THEME_LABELS[0];
        for (int i = 0; i < THEME_VALUES.length; i++) {
            if (THEME_VALUES[i].equals(current)) {
                label = THEME_LABELS[i];
                break;
            }
        }
        ((android.widget.TextView) findViewById(R.id.theme_subtitle)).setText(label);
    }

    private void showThemePicker() {
        String current = Prefs.getString(this, Prefs.KEY_THEME_MODE, "system");
        int selected = 0;
        for (int i = 0; i < THEME_VALUES.length; i++) {
            if (THEME_VALUES[i].equals(current)) {
                selected = i;
                break;
            }
        }
        new MaterialAlertDialogBuilder(this)
                .setTitle("Theme")
                .setSingleChoiceItems(THEME_LABELS, selected, (dialog, which) -> {
                    Prefs.putString(this, Prefs.KEY_THEME_MODE, THEME_VALUES[which]);
                    dialog.dismiss();
                    applyThemeMode(THEME_VALUES[which]);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void applyThemeMode(String mode) {
        int nightMode;
        switch (mode) {
            case "light":
                nightMode = AppCompatDelegate.MODE_NIGHT_NO;
                break;
            case "dark":
                nightMode = AppCompatDelegate.MODE_NIGHT_YES;
                break;
            default:
                nightMode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(nightMode);
        updateThemeSubtitle();
    }
}
