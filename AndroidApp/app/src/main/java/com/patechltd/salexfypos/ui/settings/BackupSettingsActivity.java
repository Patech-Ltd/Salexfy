package com.patechltd.salexfypos.ui.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.backup.BackupManager;
import com.patechltd.salexfypos.drive.DriveBackupEntry;
import com.patechltd.salexfypos.drive.DriveBackupManager;
import com.patechltd.salexfypos.drive.DriveConfig;
import com.patechltd.salexfypos.drive.DriveServiceHelper;
import com.patechltd.salexfypos.util.DateUtil;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.NumberUtil;
import com.patechltd.salexfypos.util.Prefs;
import com.patechltd.salexfypos.util.StorageUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BackupSettingsActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private SwitchMaterial autoBackup;
    private TextInputEditText backupInterval;
    private TextView lastBackup;

    private SwitchMaterial driveBackup;
    private TextView driveAccount;
    private TextView lastDriveBackup;
    private MaterialButton btnDriveBackupNow;
    private MaterialButton btnDriveRestoreLatest;
    private MaterialButton btnDriveList;
    private MaterialButton btnDriveRemove;
    private TextView driveRemovalHint;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_settings);

        ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar))
                .setNavigationOnClickListener(v -> finish());

        autoBackup = findViewById(R.id.switch_auto_backup);
        backupInterval = findViewById(R.id.input_backup_interval);
        lastBackup = findViewById(R.id.last_backup);

        driveBackup = findViewById(R.id.switch_drive_backup);
        driveAccount = findViewById(R.id.drive_account);
        lastDriveBackup = findViewById(R.id.last_drive_backup);
        btnDriveBackupNow = findViewById(R.id.btn_drive_backup_now);
        btnDriveRestoreLatest = findViewById(R.id.btn_drive_restore_latest);
        btnDriveList = findViewById(R.id.btn_drive_list);
        btnDriveRemove = findViewById(R.id.btn_drive_remove);
        driveRemovalHint = findViewById(R.id.drive_removal_hint);

        load();

        ((MaterialButton) findViewById(R.id.btn_backup_now)).setOnClickListener(v -> backupNow());
        ((MaterialButton) findViewById(R.id.btn_restore)).setOnClickListener(v -> restore());
        ((MaterialButton) findViewById(R.id.btn_drive_sign_in)).setOnClickListener(v -> signInDrive());
        ((MaterialButton) findViewById(R.id.btn_drive_backup_now)).setOnClickListener(v -> driveBackupNow());
        ((MaterialButton) findViewById(R.id.btn_drive_restore_latest)).setOnClickListener(v -> driveRestoreLatest());
        ((MaterialButton) findViewById(R.id.btn_drive_list)).setOnClickListener(v -> driveList());
        ((MaterialButton) findViewById(R.id.btn_drive_remove)).setOnClickListener(v -> removeDrive());
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
        String email = Prefs.getString(this, Prefs.KEY_BACKUP_DRIVE_EMAIL, null);
        boolean signedIn = email != null;
        driveAccount.setText(signedIn ? "Signed in: " + email : "Not signed in");
        driveBackup.setEnabled(signedIn);
        driveBackup.setChecked(signedIn && Prefs.getBoolean(this, Prefs.KEY_BACKUP_DRIVE_ENABLED, false));
        btnDriveBackupNow.setEnabled(signedIn);
        btnDriveRestoreLatest.setEnabled(signedIn);
        btnDriveList.setEnabled(signedIn);
        btnDriveRemove.setVisibility(android.view.View.VISIBLE);
        driveRemovalHint.setVisibility(signedIn ? android.view.View.VISIBLE : android.view.View.GONE);
        long last = Prefs.getLong(this, Prefs.KEY_LAST_DRIVE_BACKUP, 0);
        lastDriveBackup.setText("Last Drive backup: " + (last == 0
                ? "Never" : DateUtil.formatDate(last) + " " + DateUtil.formatTime(last)));
    }

    private void save() {
        Prefs.putBoolean(this, Prefs.KEY_BACKUP_ENABLED, autoBackup.isChecked());
        int hours = (int) NumberUtil.parse(
                backupInterval.getText() == null ? "" : backupInterval.getText().toString(), 6);
        Prefs.putInt(this, Prefs.KEY_BACKUP_INTERVAL_HOURS, Math.max(1, hours));
        String email = Prefs.getString(this, Prefs.KEY_BACKUP_DRIVE_EMAIL, null);
        Prefs.putBoolean(this, Prefs.KEY_BACKUP_DRIVE_ENABLED,
                email != null && driveBackup.isChecked());
        DialogUtil.toast(this, "Settings saved");
        finish();
    }

    private GoogleSignInOptions signInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(com.google.api.services.drive.DriveScopes.DRIVE_FILE))
                .build();
    }

    private void signInDrive() {
        AppLogger.i("DRIVE: starting sign-in");
        String sha1 = readSha1();
        AppLogger.i("DRIVE: package=" + getPackageName() + " sha1=" + sha1);
        try {
            int n = getResources().getIdentifier("google_app_id", "string", getPackageName());
            AppLogger.i("DRIVE: google_app_id=" + (n == 0 ? "MISSING" : getString(n)));
        } catch (Exception e) {
            AppLogger.e("DRIVE: google_app_id read failed: " + e.getMessage());
        }
        GoogleSignInOptions opts = signInOptions();
        AppLogger.i("DRIVE: sign-in options method=DEFAULT_SIGN_IN "
                + "requestIdToken=" + (opts.getServerClientId() != null)
                + " serverClientId=" + opts.getServerClientId());
        GoogleSignInClient client = GoogleSignIn.getClient(this, opts);
        startActivityForResult(client.getSignInIntent(), RC_SIGN_IN);
    }

    private String readSha1() {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-1");
            android.content.pm.PackageInfo info = getPackageManager()
                    .getPackageInfo(getPackageName(),
                            android.content.pm.PackageManager.GET_SIGNATURES);
            for (android.content.pm.Signature s : info.signatures) {
                md.update(s.toByteArray());
            }
            byte[] digest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(Character.toUpperCase(Character.forDigit((b >> 4) & 0xf, 16)));
                sb.append(Character.toUpperCase(Character.forDigit(b & 0xf, 16)));
                if (sb.length() % 3 == 2) sb.append(':');
            }
            return sb.toString();
        } catch (Exception e) {
            AppLogger.e("DRIVE: readSha1 failed: " + e.getMessage());
            return "UNKNOWN";
        }
    }

    private void removeDrive() {
        DialogUtil.confirm(this, "Disconnect Google Drive",
                "This signs out of Google Drive on this phone and clears the saved account "
                        + "and the automatic backup setting. Your backups stay in Drive. Continue?",
                () -> {
                    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions());
                    client.signOut().addOnCompleteListener(task -> {
                        Prefs.remove(this, Prefs.KEY_BACKUP_DRIVE_EMAIL);
                        Prefs.remove(this, Prefs.KEY_BACKUP_DRIVE_ENABLED);
                        Prefs.remove(this, Prefs.KEY_LAST_DRIVE_BACKUP);
                        updateDriveSection();
                        DialogUtil.toast(this, "Google Drive disconnected");
                    });
                });
    }

    /*private GoogleSignInAccount currentAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null || account.getEmail() == null) return null;
        if (!DriveServiceHelper.hasDriveScope(account)) return null;
        return account;
    }*/

    private GoogleSignInAccount currentAccount() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account == null) {
            AppLogger.i("DRIVE: currentAccount -> getLastSignedInAccount is null");
            return null;
        }
        if (account.getEmail() == null) {
            AppLogger.i("DRIVE: currentAccount -> email null, id=" + account.getId());
            return null;
        }
        if (!DriveServiceHelper.hasDriveScope(account)) {
            AppLogger.i("DRIVE: currentAccount -> missing drive scope, granted=" + account.getGrantedScopes());
            return null;
        }
        return account;
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
        GoogleSignInAccount account = currentAccount();
        if (account == null) {
            DialogUtil.toast(this, "Sign in to Google Drive first");
            return;
        }
        btnDriveBackupNow.setEnabled(false);
        new Thread(() -> {
            try {
                boolean ok = DriveBackupManager.backup(self,
                        DriveServiceHelper.getDrive(self, account));
                runOnUiThread(() -> {
                    btnDriveBackupNow.setEnabled(true);
                    updateDriveSection();
                    DialogUtil.toast(self, ok ? "Backup uploaded to Drive" : "Drive backup failed");
                });
            } catch (Exception e) {
                e.printStackTrace();
                DriveServiceHelper.log("backup now", e);
                runOnUiThread(() -> {
                    btnDriveBackupNow.setEnabled(true);
                    DialogUtil.toast(self, "Drive backup failed: " + e.getMessage());
                });
            }
        }).start();
    }

    private void driveRestoreLatest() {
        final BackupSettingsActivity self = this;
        GoogleSignInAccount account = currentAccount();
        if (account == null) {
            DialogUtil.toast(this, "Sign in to Google Drive first");
            return;
        }
        DialogUtil.confirm(this, "Restore latest from Drive",
                "This will REPLACE all current data with the newest Drive backup, after "
                        + "uploading a safety copy (slex_db_before_restore_...) of the current data. Continue?",
                () -> new Thread(() -> {
                    try {
                        boolean ok = DriveBackupManager.restoreLatest(self,
                                DriveServiceHelper.getDrive(self, account));
                        runOnUiThread(() -> DialogUtil.toast(self,
                                ok ? "Database restored. Restart the app." : "Restore failed"));
                    } catch (Exception e) {
                        DriveServiceHelper.log("restore latest", e);
                        runOnUiThread(() -> DialogUtil.toast(self, "Restore failed: " + e.getMessage()));
                    }
                }).start());
    }

    private void driveList() {
        final BackupSettingsActivity self = this;
        GoogleSignInAccount account = currentAccount();
        if (account == null) {
            DialogUtil.toast(this, "Sign in to Google Drive first");
            return;
        }
        DialogUtil.toast(this, "Loading Drive backups...");
        new Thread(() -> {
            try {
                List<DriveBackupEntry> all = DriveBackupManager.listBackups(self,
                        DriveServiceHelper.getDrive(self, account));
                runOnUiThread(() -> showDriveList(all));
            } catch (Exception e) {
                DriveServiceHelper.log("list", e);
                runOnUiThread(() -> DialogUtil.toast(self, "Failed to list backups: " + e.getMessage()));
            }
        }).start();
    }

    private void showDriveList(List<DriveBackupEntry> all) {
        if (all.isEmpty()) {
            DialogUtil.toast(this, "No Drive backups found");
            return;
        }
        List<String> safeNames = new ArrayList<>();
        for (DriveBackupEntry e : all) {
            safeNames.add(e.name + "  (" + formatCreated(e.createdTime) + ")");
        }
        String[] options = safeNames.toArray(new String[0]);
        DialogUtil.pick(this, "Drive backups", options, -1, index -> {
            if (index < 0 || index >= all.size()) return;
            restoreNamed(all.get(index).name);
        });
    }

    private String formatCreated(long created) {
        if (created <= 0) return "?";
        return DateUtil.formatDate(created) + " " + DateUtil.formatTime(created);
    }

    private void restoreNamed(String name) {
        final BackupSettingsActivity self = this;
        GoogleSignInAccount account = currentAccount();
        if (account == null) return;
        DialogUtil.confirm(this, "Restore backup",
                "Restore \"" + name + "\"? A safety copy of the current data will be uploaded first.",
                () -> new Thread(() -> {
                    try {
                        boolean ok = DriveBackupManager.restoreNamed(self,
                                DriveServiceHelper.getDrive(self, account), name);
                        runOnUiThread(() -> DialogUtil.toast(self,
                                ok ? "Database restored. Restart the app." : "Restore failed"));
                    } catch (Exception e) {
                        DriveServiceHelper.log("restore named", e);
                        runOnUiThread(() -> DialogUtil.toast(self, "Restore failed: " + e.getMessage()));
                    }
                }).start());
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
        if (requestCode == RC_SIGN_IN) {
            AppLogger.i("DRIVE: sign-in result resultCode=" + resultCode
                    + " data=" + (data == null ? "null" : String.valueOf(data)));
            if (data != null && data.getExtras() != null) {
                for (String key : data.getExtras().keySet()) {
                    try {
                        Object v = data.getExtras().get(key);
                        AppLogger.i("DRIVE: intent extra " + key + "=" + v);
                    } catch (Exception ignored) {
                    }
                }
            }
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
                if (account != null && account.getEmail() != null) {
                    AppLogger.i("DRIVE: sign-in SUCCESS email=" + account.getEmail()
                            + " id=" + account.getId()
                            + " scopes=" + account.getGrantedScopes());
                    Prefs.putString(this, Prefs.KEY_BACKUP_DRIVE_EMAIL, account.getEmail());
                    updateDriveSection();
                    DialogUtil.toast(this, "Signed in to Google Drive");
                } else {
                    AppLogger.e("DRIVE: sign-in returned null account (resultCode="
                            + resultCode + " data=" + data + ")");
                    if (data != null && data.getExtras() != null) {
                        Object raw = data.getExtras().get("googleSignInAccount");
                        if (raw instanceof GoogleSignInAccount) {
                            GoogleSignInAccount acc = (GoogleSignInAccount) raw;
                            AppLogger.i("DRIVE: raw account email=" + acc.getEmail()
                                    + " id=" + acc.getId()
                                    + " scopeStrings=" + acc.getGrantedScopes()
                                    + " serverAuthCode=" + acc.getServerAuthCode());
                        } else {
                            AppLogger.i("DRIVE: raw googleSignInAccount absent/other ("
                                    + (raw == null ? "null" : raw.getClass().getName()) + ")");
                        }
                    }
                    GoogleSignInAccount last = GoogleSignIn.getLastSignedInAccount(this);
                    if (last != null) {
                        AppLogger.i("DRIVE: lastSignedIn email=" + last.getEmail()
                                + " id=" + last.getId()
                                + " scopes=" + last.getGrantedScopes());
                    } else {
                        AppLogger.i("DRIVE: no last signed-in account");
                    }
                    showSignInError(resultCode == RESULT_OK ? 10 : 13);
                }
            } catch (ApiException e) {
                AppLogger.e("DRIVE: ApiException status=" + e.getStatusCode()
                        + " statusMessage=" + e.getStatusMessage()
                        + " msg=" + e.getMessage(), e);
                showSignInError(e.getStatusCode());
            } catch (Exception e) {
                AppLogger.e("DRIVE: unexpected " + e.getClass().getSimpleName()
                        + " " + e.getMessage(), e);
                showSignInError(0);
            }
            return;
        }
        if (requestCode == StorageUtil.REQ_BACKUP && resultCode == RESULT_OK
                && data != null && data.getData() != null) {
            final Uri uri = data.getData();
            DialogUtil.confirm(this, "Restore database",
                    "This will REPLACE all current data with the backup. Continue?",
                    () -> new Thread(() -> {
                        boolean ok = BackupManager.restoreFromUri(BackupSettingsActivity.this, uri);
                        runOnUiThread(() -> DialogUtil.toast(BackupSettingsActivity.this,
                                ok ? "Database restored. Restart the app." : "Restore failed"));
                    }).start());
        }
    }

    private void showSignInError(int statusCode) {
        String message;
        switch (statusCode) {
            case 4:
                message = "Google Play services needs an update or isn't available on this phone.\n"
                        + "Open the Play Store and update 'Google Play services', then try again.";
                break;
            case 7:
                message = "No Google account is set up on this phone.\n"
                        + "Go to Settings → Accounts, add your Google account, then try again.";
                break;
            case 8:
                message = "The Google sign-in screen didn't complete.\nPlease tap 'Sign in to Google Drive' again.";
                break;
            case 10:
                message = "Google couldn't complete the Drive sign-in (code 10).\n"
                        + "This usually means the app isn't set up for Drive on this phone yet.\n"
                        + "Update Google Play services, make sure you're signed into a Google account, "
                        + "and try again. If it keeps failing, contact the app provider.";
                break;
            case 12501:
                message = "You cancelled, or Google rejected the Drive permission.\n"
                        + "Use the SAME Google account that is on this phone, and approve all permissions.";
                break;
            default:
                message = "Drive sign-in failed (code " + statusCode + ").\n"
                        + "Update Google Play services, add a Google account to this phone in Settings → Accounts, "
                        + "then try again.";
                break;
        }
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Drive sign-in failed")
                .setMessage(message)
                .setPositiveButton("Try again", (d, w) -> signInDrive())
                .setNegativeButton("Cancel", null)
                .show();
    }
}
