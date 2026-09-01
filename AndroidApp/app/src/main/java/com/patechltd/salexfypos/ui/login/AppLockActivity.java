package com.patechltd.salexfypos.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.security.PasswordHasher;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.Prefs;

import java.util.concurrent.Executor;

public class AppLockActivity extends AppCompatActivity {

    private MaterialButton btnFingerprint;
    private TextInputEditText passwordField;
    private MaterialButton btnUnlock;
    private ProgressBar progress;

    private int failedAttempts = 0;
    private long lockoutUntil = 0;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MS = 30_000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_lock);

        btnFingerprint = findViewById(R.id.btn_fingerprint);
        passwordField = findViewById(R.id.password);
        btnUnlock = findViewById(R.id.btn_unlock);
        progress = findViewById(R.id.progress);
        TextView shopName = findViewById(R.id.shop_name);

        shopName.setText(Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop"));

        boolean biometricEnabled = Prefs.getBoolean(this, Prefs.KEY_APP_LOCK_USE_BIOMETRIC, true);
        boolean biometricAvailable = isBiometricAvailable();

        if (biometricEnabled && biometricAvailable) {
            btnFingerprint.setVisibility(View.VISIBLE);
            btnFingerprint.setOnClickListener(v -> showBiometricPrompt());
            showBiometricPrompt();
        } else {
            btnFingerprint.setVisibility(View.GONE);
        }

        btnUnlock.setOnClickListener(v -> verifyPassword());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                moveTaskToBack(true);
            }
        });
    }

    private boolean isBiometricAvailable() {
        BiometricManager bm = BiometricManager.from(this);
        int result = bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK);
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    private void showBiometricPrompt() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt prompt = new BiometricPrompt(this, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        unlock();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        Toast.makeText(AppLockActivity.this, "Fingerprint not recognized", Toast.LENGTH_SHORT).show();
                    }
                });

        BiometricPrompt.PromptInfo info = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock " + Prefs.getString(this, Prefs.KEY_SHOP_NAME, "App"))
                .setSubtitle("Use fingerprint to continue")
                .setNegativeButtonText("Use password")
                .build();

        prompt.authenticate(info);
    }

    private void verifyPassword() {
        long now = System.currentTimeMillis();
        if (now < lockoutUntil) {
            long remaining = (lockoutUntil - now) / 1000;
            Toast.makeText(this, "Too many attempts. Wait " + remaining + "s", Toast.LENGTH_SHORT).show();
            return;
        }

        String pwd = passwordField.getText() != null ? passwordField.getText().toString() : "";
        if (pwd.isEmpty()) {
            Toast.makeText(this, "Enter your password", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = Session.userId(this);
        if (userId == null) {
            redirectToLogin();
            return;
        }

        btnUnlock.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        Repository repo = Repository.get(this);
        repo.run(() -> {
            User user = repo.admin.getUser(userId);
            Handler handler = new Handler(Looper.getMainLooper());
            if (user == null || !PasswordHasher.verify(pwd, user.salt, user.passwordHash)) {
                failedAttempts++;
                if (failedAttempts >= MAX_FAILED_ATTEMPTS) {
                    lockoutUntil = System.currentTimeMillis() + LOCKOUT_DURATION_MS;
                    failedAttempts = 0;
                }
                handler.post(() -> {
                    btnUnlock.setEnabled(true);
                    progress.setVisibility(View.GONE);
                    passwordField.setText("");
                    if (lockoutUntil > System.currentTimeMillis()) {
                        Toast.makeText(this, "Account locked for 30 seconds", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Incorrect password (" + failedAttempts + "/" + MAX_FAILED_ATTEMPTS + ")",
                                Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                handler.post(this::unlock);
            }
        });
    }

    private void unlock() {
        long now = System.currentTimeMillis();
        Prefs.putLong(this, Prefs.KEY_APP_LOCK_LAST_UNLOCK, now);
        Prefs.putBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, false);

        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

    private void redirectToLogin() {
        Session.end(this);
        Prefs.putBoolean(this, Prefs.KEY_APP_LOCK_NEEDS_REAUTH, false);
        startActivity(new Intent(this, LoginActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        passwordField.setText("");
    }
}
