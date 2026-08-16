package com.patechltd.salexfypos.ui.users;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.RoleAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.RoleAuthorities;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.NumberUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class RoleActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private RoleAdapter adapter;
    private List<Role> roles = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role);
        repo = Repository.get(this);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        RecyclerView list = findViewById(R.id.role_list);
        adapter = new RoleAdapter(position -> editRole(position));
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        findViewById(R.id.btn_add_role).setOnClickListener(v -> editRole(-1));
        load();
    }

    private void load() {
        repo.run(() -> {
            roles = repo.admin.getRoles();
            List<RoleAdapter.Row> rows = new ArrayList<>();
            for (Role r : roles) {
                RoleAdapter.Row row = new RoleAdapter.Row();
                row.name = r.roleName;
                row.subtitle = RoleAuthorities.fromJson(r.authoritiesJson).size() + " permissions" +
                        (r.commissionPercent > 0 ? " · " + r.commissionPercent + "% commission" : "");
                rows.add(row);
            }
            handler.post(() -> adapter.submit(rows));
        });
    }

    private void editRole(int position) {
        Role role;
        boolean isNew = position < 0 || position >= roles.size();
        if (isNew) {
            role = new Role();
            role.uid = UUID.randomUUID().toString();
            role.createdAt = System.currentTimeMillis();
        } else {
            role = roles.get(position);
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_role_edit, null);
        TextInputEditText nameInput = dialogView.findViewById(R.id.role_name_input);
        TextInputEditText commissionInput = dialogView.findViewById(R.id.role_commission_input);
        LinearLayout perms = dialogView.findViewById(R.id.permission_list);
        nameInput.setText(role.roleName);
        if (!isNew && role.commissionPercent > 0) {
            commissionInput.setText(String.valueOf(role.commissionPercent));
        }

        Set<String> selected = new HashSet<>(RoleAuthorities.nameSet(RoleAuthorities.fromJson(role.authoritiesJson)));
        List<CheckBox> boxes = new ArrayList<>();
        for (Authority a : Authority.values()) {
            CheckBox box = new CheckBox(this);
            box.setText(a.getLabel());
            box.setChecked(selected.contains(a.name()));
            perms.addView(box);
            boxes.add(box);
        }

        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                .setTitle(isNew ? "Add Role" : "Edit Role")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) ->
                        saveRole(role, isNew, nameInput, commissionInput, boxes))
                .setNegativeButton("Cancel", null);
        if (!isNew) {
            builder.setNeutralButton("Delete", (dialog, which) -> deleteRole(role));
        }
        builder.show();
    }

    private void saveRole(Role role, boolean isNew, TextInputEditText nameInput,
                          TextInputEditText commissionInput, List<CheckBox> boxes) {
        String n = nameInput.getText() == null ? "" : nameInput.getText().toString().trim();
        if (n.isEmpty()) {
            DialogUtil.toast(this, "Role name required");
            return;
        }
        double comm = NumberUtil.parse(
                commissionInput.getText() == null ? "" : commissionInput.getText().toString(), 0);
        List<Authority> list = new ArrayList<>();
        for (int i = 0; i < boxes.size(); i++) {
            if (boxes.get(i).isChecked()) list.add(Authority.values()[i]);
        }
        role.roleName = n;
        role.commissionPercent = comm;
        role.authoritiesJson = RoleAuthorities.toJson(list);
        repo.run(() -> {
            if (isNew) {
                repo.admin.insertRole(role);
            } else {
                repo.admin.updateRole(role);
            }
            refreshPermissions();
            handler.post(this::load);
        });
    }

    private void deleteRole(Role role) {
        if (role.isDefault) {
            DialogUtil.toast(this, "The default Administrator role cannot be deleted");
            return;
        }
        DialogUtil.confirm(this, "Delete role " + role.roleName + "?",
                "Users assigned this role will keep their access until reassigned.",
                () -> repo.run(() -> {
                    int inUse = repo.admin.userCountByRole(role.uid);
                    if (inUse > 0) {
                        handler.post(() -> DialogUtil.toast(this,
                                "Role is assigned to " + inUse + " user(s). Reassign them first."));
                        return;
                    }
                    repo.admin.deleteRole(role);
                    refreshPermissions();
                    handler.post(this::load);
                }));
    }

    private void refreshPermissions() {
        PermissionChecker.invalidate();
        String userId = Session.userId(this);
        if (userId != null) PermissionChecker.loadAsync(this, userId);
    }
}
