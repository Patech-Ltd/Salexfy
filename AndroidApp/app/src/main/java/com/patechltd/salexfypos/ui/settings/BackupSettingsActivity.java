package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.backup.BackupManager;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;

import java.io.File;

public class BackupSettingsActivity extends AppCompatActivity {

    private SwitchMaterial autoBackup;
    private TextInputEditText backupInterval;
    private TextView lastBackup;

    private SwitchMaterial driveBackup;
    private TextView driveFolder;
    private TextView lastDriveBackup;
    private MaterialButton btnDriveBackupNow;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_settings);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        autoBackup = findViewById(R.id.switch_auto_backup);
        backupInterval = findViewById(R.id.input_backup_interval);
        lastBackup = findViewById(R.id.last_backup);

        driveBackup = findViewById(R.id.switch_drive_backup);
        driveFolder = findViewById(R.id.drive_folder);
        lastDriveBackup = findViewById(R.id.last_drive_backup);
        btnDriveBackupNow = findViewById(R.id.btn_drive_backup_now);

        load();

        ((MaterialButton) findViewById(R.id.btn_backup_now)).setOnClickListener(v -> backupNow());
        ((MaterialButton) findViewById(R.id.btn_restore)).setOnClickListener(v -> restore());
        ((MaterialButton) findViewById(R.id.btn_pick_drive_folder)).setOnClickListener(v -> pickDriveFolder());
        ((MaterialButton) findViewById(R.id.btn_drive_backup_now)).setOnClickListener(v -> driveBackupNow());
        ((MaterialButton) findViewById(R.id.btn_save)).setOnClickListener(v -> save());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLastBackup();
        updateDriveSection();
    }

    private void load() {
        autoBackup.setChecked(Prefs.getBoolean(this, Prefs.KEY_BACKUP_ENABLED, true));
        backupInterval.setText(String.valueOf(
                Prefs.getInt(this, Prefs.KEY_BACKUP_INTERVAL_HOURS, 6)));
        updateLastBackup();
        updateDriveSection();
    }

    private void updateLastBackup() {
        long last = Prefs.getLong(this, Prefs.KEY_LAST_BACKUP, 0);
        lastBackup.setText("Last backup: " + (last == 0
                ? "Never" : DateUtil.formatDate(last) + " " + DateUtil.formatTime(last)));
    }

    private void updateDriveSection() {
        String uriString = Prefs.getString(this, Prefs.KEY_BACKUP_DRIVE_URI, null);
        boolean hasFolder = uriString != null && !uriString.isEmpty();
        driveFolder.setText(hasFolder ? "Folder: " + folderName(uriString) : "No folder selected");
        driveBackup.setEnabled(hasFolder);
        driveBackup.setChecked(hasFolder && Prefs.getBoolean(this, Prefs.KEY_BACKUP_DRIVE_ENABLED, false));
        btnDriveBackupNow.setEnabled(hasFolder);
        long last = Prefs.getLong(this, Prefs.KEY_LAST_DRIVE_BACKUP, 0);
        lastDriveBackup.setText("Last Drive backup: " + (last == 0
                ? "Never" : DateUtil.formatDate(last) + " " + DateUtil.formatTime(last)));
    }

    private String folderName(String uriString) {
        try {
            Uri treeUri = Uri.parse(uriString);
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri,
                    DocumentsContract.getTreeDocumentId(treeUri));
            try (Cursor cursor = getContentResolver().query(docUri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME);
                    if (idx >= 0) {
                        String name = cursor.getString(idx);
                        if (name != null && !name.isEmpty()) return name;
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return uriString;
    }

    private void save() {
        Prefs.putBoolean(this, Prefs.KEY_BACKUP_ENABLED, autoBackup.isChecked());
        int hours = (int) NumberUtil.parse(
                backupInterval.getText() == null ? "" : backupInterval.getText().toString(), 6);
        Prefs.putInt(this, Prefs.KEY_BACKUP_INTERVAL_HOURS, Math.max(1, hours));
        String uriString = Prefs.getString(this, Prefs.KEY_BACKUP_DRIVE_URI, null);
        Prefs.putBoolean(this, Prefs.KEY_BACKUP_DRIVE_ENABLED,
                uriString != null && !uriString.isEmpty() && driveBackup.isChecked());
        DialogUtil.toast(this, "Settings saved");
        finish();
    }

    private void pickDriveFolder() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION
                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, StorageUtil.REQ_DRIVE_FOLDER);
    }

    private void backupNow() {
        final BackupSettingsActivity self = this;
        new Thread(() -> {
            File target = BackupManager.timestampedBackupFile(self);
            boolean ok = BackupManager.backupToFile(self, target);
            runOnUiThread(() -> {
                updateLastBackup();
                DialogUtil.toast(self, ok ? "Backup saved to internal storage" : "Backup failed");
            });
        }).start();
    }

    private void driveBackupNow() {
        final BackupSettingsActivity self = this;
        btnDriveBackupNow.setEnabled(false);
        new Thread(() -> {
            boolean ok = BackupManager.backupToDrive(self);
            runOnUiThread(() -> {
                btnDriveBackupNow.setEnabled(true);
                updateDriveSection();
                DialogUtil.toast(self, ok ? "Backup uploaded to Drive" : "Drive backup failed");
            });
        }).start();
    }

    private void restore() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        startActivityForResult(intent, StorageUtil.REQ_BACKUP);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == StorageUtil.REQ_DRIVE_FOLDER && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            Uri treeUri = data.getData();
            getContentResolver().takePersistableUriPermission(treeUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            Prefs.putString(this, Prefs.KEY_BACKUP_DRIVE_URI, treeUri.toString());
            updateDriveSection();
            return;
        }
        if (requestCode == StorageUtil.REQ_BACKUP && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            final android.net.Uri uri = data.getData();
            DialogUtil.confirm(this, "Restore database",
                    "This will REPLACE all current data with the backup. Continue?",
                    () -> new Thread(() -> {
                        boolean ok = BackupManager.restoreFromUri(BackupSettingsActivity.this, uri);
                        runOnUiThread(() -> DialogUtil.toast(BackupSettingsActivity.this,
                                ok ? "Database restored. Restart the app." : "Restore failed"));
                    }).start());
        }
    }
}
