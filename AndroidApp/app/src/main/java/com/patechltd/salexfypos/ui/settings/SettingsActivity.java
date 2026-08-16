package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.patechltd.salexfypos.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        findViewById(R.id.row_business).setOnClickListener(
                v -> startActivity(new Intent(this, BusinessSettingsActivity.class)));
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
    }
}
