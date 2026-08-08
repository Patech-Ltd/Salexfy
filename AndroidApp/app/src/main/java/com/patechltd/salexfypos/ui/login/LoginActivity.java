package com.patechltd.salexfypos.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.security.PasswordHasher;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.Prefs;

public class LoginActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Repository repo = Repository.get(this);
        if (Session.isLoggedIn(this)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        setContentView(R.layout.activity_login);

        repo.io(repo.admin::userCount).thenAcceptAsync(count -> {
            if (count > 0) return;
            startActivity(new Intent(this, SetupActivity.class));
            finish();
        }, new Handler(Looper.getMainLooper())::post);

        EditText username = findViewById(R.id.username);
        EditText password = findViewById(R.id.password);
        MaterialButton loginBtn = findViewById(R.id.login_btn);
        ProgressBar progress = findViewById(R.id.progress);
        TextView shopName = findViewById(R.id.shop_name);
        TextView shopTag = findViewById(R.id.shop_tag);

        String shop = Prefs.getString(this, Prefs.KEY_SHOP_NAME, "My Shop");
        shopName.setText(shop);
        shopTag.setText("Retail • Wholesale • Point of Sale");

        loginBtn.setOnClickListener(v -> {
            String u = username.getText().toString().trim();
            String p = password.getText().toString();
            if (u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, "Enter username and password", Toast.LENGTH_SHORT).show();
                return;
            }
            loginBtn.setEnabled(false);
            progress.setVisibility(View.VISIBLE);
            repo.run(() -> {
                User user = repo.admin.findByUsername(u);
                Handler handler = new Handler(Looper.getMainLooper());
                if (user == null || !PasswordHasher.verify(p, user.salt, user.passwordHash)) {
                    handler.post(() -> {
                        loginBtn.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
                        password.setText("");
                    });
                    return;
                }
                if (!user.isActive) {
                    handler.post(() -> {
                        loginBtn.setEnabled(true);
                        progress.setVisibility(View.GONE);
                        Toast.makeText(this, "This account is disabled", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                handler.post(() -> {
                    Session.start(this, user);
                    PermissionChecker.loadAsync(this, user.id);
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            });
        });
    }
}
