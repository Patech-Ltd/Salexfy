package com.patechltd.salexfypos.ui.settings;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.BackupAdapter;
import com.patechltd.salexfypos.backup.BackupManager;
import com.patechltd.salexfypos.drive.DriveBackupEntry;
import com.patechltd.salexfypos.drive.DriveBackupManager;
import com.patechltd.salexfypos.drive.DriveConfig;
import com.patechltd.salexfypos.drive.DriveServiceHelper;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lists every backup on one page: Google Drive copies (regular + safety) and the
 * backups stored on this device. Restoring always goes through a two-step warning.
 */
public class BackupsActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BackupAdapter adapter;
    private ProgressBar progress;
    private TextView emptyText;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backups);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        progress = findViewById(R.id.progress);
        emptyText = findViewById(R.id.empty_text);
        RecyclerView list = findViewById(R.id.backup_list);
        adapter = new BackupAdapter();
        adapter.setListener(row -> onRestore(row));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        progress.setVisibility(View.VISIBLE);
        emptyText.setVisibility(View.GONE);
        final BackupsActivity self = this;
        new Thread(() -> {
            List<DriveBackupEntry> drive = new ArrayList<>();
            GoogleSignInAccount account = signedInAccount();
            if (account != null) {
                try {
                    drive = DriveBackupManager.listBackups(self,
                            DriveServiceHelper.getDrive(self, account));
                } catch (Exception e) {
                    DriveServiceHelper.log("list backups", e);
                }
            }

            File dir = BackupManager.defaultBackupDir(self);
            File[] files = dir.listFiles((d, name) ->
                    name.startsWith("salexfy_backup_") && name.endsWith(".db"));
            final List<File> device = files == null ? new ArrayList<>()
                    : new ArrayList<>(Arrays.asList(files));
            Collections.sort(device, Comparator.comparingLong(File::lastModified).reversed());

            final boolean signedIn = account != null;
            final List<DriveBackupEntry> driveCopy = drive;
            handler.post(() -> render(driveCopy, device, signedIn));
        }).start();
    }

    private void render(List<DriveBackupEntry> drive, List<File> device, boolean signedIn) {
        progress.setVisibility(View.GONE);
        List<BackupAdapter.Row> rows = new ArrayList<>();

        if (!drive.isEmpty()) {
            rows.add(new BackupAdapter.Row("Google Drive"));
            for (DriveBackupEntry e : drive) {
                rows.add(new BackupAdapter.Row(
                        friendlyTime(e.createdTime),
                        "Google Drive" + (isSafety(e.name) ? " \u00b7 safety copy" : "")
                                + " \u00b7 " + formatSize(e.sizeBytes),
                        e.name, e));
            }
        } else if (!signedIn) {
            rows.add(new BackupAdapter.Row("Google Drive"));
            rows.add(new BackupAdapter.Row("Not signed in to Google Drive",
                    "Sign in from Settings \u2192 Backup & Restore to see cloud backups",
                    null, null));
        }

        rows.add(new BackupAdapter.Row("On this device"));
        if (device.isEmpty()) {
            rows.add(new BackupAdapter.Row("No backups on this device",
                    "Use the 'Backup now' or Drive backup button in Settings to create one",
                    null, null));
        }
        for (File f : device) {
            rows.add(new BackupAdapter.Row(
                    DateUtil.format(f.lastModified()),
                    "This device" + (f.getName().startsWith("salexfy_backup_before_restore_")
                            ? " \u00b7 safety copy" : "") + " \u00b7 " + formatSize(f.length()),
                    f.getName(), f));
        }

        boolean hasItems = rows.stream().anyMatch(r -> !r.header && r.payload != null);
        if (!hasItems) {
            emptyText.setText(signedIn
                    ? "No backups found yet. Press the backup button in Settings to create one."
                    : "No backups found. Sign in to Google Drive or run a backup to get started.");
            emptyText.setVisibility(View.VISIBLE);
        }
        adapter.submit(rows);
    }

    private void onRestore(BackupAdapter.Row row) {
        if (row.payload == null) return;
        final Object payload = row.payload;
        final String label = row.restoreLabel == null ? row.title : row.restoreLabel;
        DialogUtil.confirmRestore(this, label, () -> new Thread(() -> {
            boolean ok;
            if (payload instanceof DriveBackupEntry) {
                ok = restoreDrive((DriveBackupEntry) payload);
            } else if (payload instanceof File) {
                ok = restoreFile((File) payload);
            } else {
                ok = false;
            }
            final boolean result = ok;
            handler.post(() -> {
                DialogUtil.toast(this, result
                        ? "Database restored. Restart the app." : "Restore failed");
                load();
            });
        }).start());
    }

    private boolean restoreDrive(DriveBackupEntry entry) {
        try {
            GoogleSignInAccount account = signedInAccount();
            if (account == null) {
                return false;
            }
            return DriveBackupManager.restoreEntry(this,
                    DriveServiceHelper.getDrive(this, account), entry);
        } catch (Exception e) {
            DriveServiceHelper.log("restore backup", e);
            return false;
        }
    }

    private boolean restoreFile(File file) {
        try {
            return BackupManager.restoreFromFile(this, file);
        } catch (Exception e) {
            return false;
        }
    }

    private GoogleSignInAccount signedInAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || account.getEmail() == null) return null;
        if (!DriveServiceHelper.hasDriveScope(account)) return null;
        return account;
    }

    private static boolean isSafety(String name) {
        return name != null
                && (name.startsWith(DriveConfig.BEFORE_RESTORE_PREFIX)
                || name.startsWith("salexfy_backup_before_restore_"));
    }

    private static String friendlyTime(long created) {
        return created <= 0 ? "Unknown time" : DateUtil.format(created);
    }

    private static String formatSize(long bytes) {
        if (bytes <= 0) return "?";
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(java.util.Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(java.util.Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }
}