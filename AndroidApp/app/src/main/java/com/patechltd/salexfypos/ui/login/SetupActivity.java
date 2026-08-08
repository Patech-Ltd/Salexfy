package com.patechltd.salexfypos.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.security.PasswordHasher;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.Prefs;

import java.util.UUID;

public class SetupActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        TextInputEditText shopName = findViewById(R.id.shop_name);
        TextInputEditText username = findViewById(R.id.username);
        TextInputEditText fullName = findViewById(R.id.full_name);
        TextInputEditText password = findViewById(R.id.password);
        TextInputEditText confirm = findViewById(R.id.confirm);
        MaterialButton createBtn = findViewById(R.id.create_btn);
        ProgressBar progress = findViewById(R.id.progress);

        createBtn.setOnClickListener(v -> {
            String shop = shopName.getText().toString().trim();
            String u = username.getText().toString().trim();
            String fn = fullName.getText().toString().trim();
            String p = password.getText().toString();
            String c = confirm.getText().toString();
            if (shop.isEmpty()) {
                toast("Enter your shop name");
                return;
            }
            if (u.isEmpty()) {
                toast("Enter an admin username");
                return;
            }
            if (fn.isEmpty()) {
                toast("Enter the admin full name");
                return;
            }
            if (p.length() < 4) {
                toast("Password must be at least 4 characters");
                return;
            }
            if (!p.equals(c)) {
                toast("Passwords do not match");
                return;
            }
            Prefs.putString(this, Prefs.KEY_SHOP_NAME, shop);
            createBtn.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            Repository repo = Repository.get(this);
            repo.run(() -> {
                User admin = repo.admin.findByUsername(u);
                Handler handler = new Handler(Looper.getMainLooper());
                if (admin != null) {
                    handler.post(() -> {
                        createBtn.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        toast("Username already exists");
                    });
                    return;
                }
                try {
                    repo.admin.insertRole(com.patechltd.salexfypos.helper.RoleHelper.adminRoleFor(repo));
                    String salt = PasswordHasher.generateSalt();
                    User user = new User();
                    user.id = UUID.randomUUID().toString();
                    user.username = u;
                    user.fullName = fn;
                    user.salt = salt;
                    user.passwordHash = PasswordHasher.hash(p, salt);
                    user.roleId = repo.admin.findRoleByName("Administrator").id;
                    user.isActive = true;
                    user.createdAt = System.currentTimeMillis();
                    repo.admin.insertUser(user);
                    Prefs.putBoolean(this, Prefs.KEY_SEEDED, true);
                    handler.post(() -> {
                        Session.start(this, user);
                        PermissionChecker.loadAsync(this, user.id);
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    });
                } catch (Exception e) {
                    handler.post(() -> {
                        createBtn.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        toast("Could not create account: " + e.getMessage());
                    });
                }
            });
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
