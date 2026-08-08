package com.patechltd.salexfypos.ui.users;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.security.PasswordHasher;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.AppLogger;
import com.patechltd.salexfypos.util.DialogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserEditActivity extends AppCompatActivity {

    private Repository repo;
    private TextInputEditText name, username, password;
    private SwitchMaterial active;
    private MaterialButton roleButton, deleteButton, editButton;
    private User user;
    private List<Role> roles = new ArrayList<>();
    private Role selectedRole;
    private boolean viewOnly;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_edit);
        repo = Repository.get(this);

        findViewById(R.id.toolbar).setOnClickListener(v -> onBackPressed());

        name = findViewById(R.id.input_name);
        username = findViewById(R.id.input_username);
        password = findViewById(R.id.input_password);
        active = findViewById(R.id.switch_active);
        roleButton = findViewById(R.id.btn_role);
        deleteButton = findViewById(R.id.btn_delete);

        roleButton.setOnClickListener(v -> pickRole());
        findViewById(R.id.btn_save).setOnClickListener(v -> save());
        deleteButton.setOnClickListener(v -> delete());
        editButton = findViewById(R.id.btn_edit);
        editButton.setVisibility(android.view.View.GONE);

        String userId = getIntent().getStringExtra("userId");
        viewOnly = getIntent().getBooleanExtra("viewOnly", false);
        editButton.setOnClickListener(v -> {
            Intent i = new Intent(this, UserEditActivity.class);
            i.putExtra("userId", userId);
            startActivity(i);
            finish();
        });

        if (userId == null) {
            ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar)).setTitle("Add User");
        } else {
            ((com.google.android.material.appbar.MaterialToolbar) findViewById(R.id.toolbar)).setTitle(
                    viewOnly ? "User Details" : "Edit User");
        }

        loadRoles(userId);
    }

    private void loadRoles(String userId) {
        repo.run(() -> {
            roles = repo.admin.getRoles();
            if (userId != null) {
                user = repo.admin.getUser(userId);
            }
            if (user != null) {
                runOnUiThread(() -> {
                    name.setText(user.fullName);
                    username.setText(user.username);
                    active.setChecked(user.isActive);
                    deleteButton.setVisibility(android.view.View.VISIBLE);
                    for (Role r : roles) {
                        if (r.id.equals(user.roleId)) {
                            selectedRole = r;
                            roleButton.setText(r.roleName);
                            break;
                        }
                    }
                    if (viewOnly) {
                        name.setEnabled(false);
                        username.setEnabled(false);
                        password.setEnabled(false);
                        active.setEnabled(false);
                        roleButton.setEnabled(false);
                        deleteButton.setVisibility(android.view.View.GONE);
                        findViewById(R.id.btn_save).setVisibility(android.view.View.GONE);
                        editButton.setVisibility(android.view.View.VISIBLE);
                    }
                });
            }
        });
    }

    private void pickRole() {
        String[] names = new String[roles.size()];
        for (int i = 0; i < roles.size(); i++) names[i] = roles.get(i).roleName;
        int sel = selectedRole == null ? -1 : roles.indexOf(selectedRole);
        DialogUtil.pick(this, "Select role", names, sel, index -> {
            selectedRole = roles.get(index);
            roleButton.setText(selectedRole.roleName);
        });
    }

    private void save() {
        String n = name.getText() == null ? "" : name.getText().toString().trim();
        String u = username.getText() == null ? "" : username.getText().toString().trim();
        String pw = password.getText() == null ? "" : password.getText().toString();
        if (n.isEmpty() || u.isEmpty() || selectedRole == null) {
            DialogUtil.toast(this, "Name, username and role are required");
            return;
        }
        repo.run(() -> {
            if (user != null) {
                if (repo.admin.findByUsernameExcluding(u, user.id) != null) {
                    runOnUiThread(() -> DialogUtil.toast(this, "Username already exists"));
                    return;
                }
                user.fullName = n;
                user.username = u;
                user.roleId = selectedRole.id;
                user.isActive = active.isChecked();
                if (!pw.isEmpty()) {
                    String salt = PasswordHasher.generateSalt();
                    user.salt = salt;
                    user.passwordHash = PasswordHasher.hash(pw, salt);
                }
                repo.admin.updateUser(user);
                runOnUiThread(() -> DialogUtil.toast(this, "User updated"));
            } else {
                if (repo.admin.findByUsername(u) != null) {
                    runOnUiThread(() -> DialogUtil.toast(this, "Username already exists"));
                    return;
                }
                User nu = new User();
                nu.id = UUID.randomUUID().toString();
                nu.fullName = n;
                nu.username = u;
                nu.roleId = selectedRole.id;
                nu.isActive = active.isChecked();
                nu.createdBy = Session.userId(this);
                nu.createdAt = System.currentTimeMillis();
                String salt = pw.isEmpty() ? PasswordHasher.generateSalt() : PasswordHasher.generateSalt();
                nu.salt = salt;
                nu.passwordHash = pw.isEmpty() ? PasswordHasher.hash("1234", salt) : PasswordHasher.hash(pw, salt);
                repo.admin.insertUser(nu);
                runOnUiThread(() -> DialogUtil.toast(this, "User created"));
            }
            runOnUiThread(this::finish);
        });
    }

    private void delete() {
        DialogUtil.confirm(this, "Delete user",
                "This account will no longer be able to log in.", () -> {
                    repo.run(() -> {
                        repo.admin.deleteUser(user);
                        runOnUiThread(this::finish);
                    });
                });
    }
}
