package com.patechltd.salexfypos.ui.users;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.adapter.UserAdapter;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserListActivity extends AppCompatActivity {

    private Repository repo;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private UserAdapter adapter;
    private List<User> users = new ArrayList<>();
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_directory_list);
        repo = Repository.get(this);

        toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Users");
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.inflateMenu(R.menu.menu_user_list);
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_roles) {
                startActivity(new Intent(this, RoleActivity.class));
                return true;
            }
            return false;
        });

        RecyclerView list = findViewById(R.id.list);
        adapter = new UserAdapter(position -> {
            if (position < 0 || position >= users.size()) return;
            Intent intent = new Intent(this, UserEditActivity.class);
            intent.putExtra("userId", users.get(position).uid);
            intent.putExtra("viewOnly", true);
            startActivity(intent);
        });
        list.setLayoutManager(new LinearLayoutManager(this));
        list.setAdapter(adapter);

        MaterialButton add = findViewById(R.id.btn_add);
        add.setText("Add User");
        add.setOnClickListener(v -> startActivity(new Intent(this, UserEditActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        MenuItem rolesItem = toolbar == null ? null : toolbar.getMenu().findItem(R.id.menu_roles);
        if (rolesItem != null) {
            rolesItem.setVisible(PermissionChecker.has(this, Authority.USER_EDIT));
        }
        load();
    }

    private void load() {
        repo.run(() -> {
            users = repo.admin.getUsers();
            Map<String, String> roleMap = new HashMap<>();
            for (Role r : repo.admin.getRoles()) roleMap.put(r.uid, r.roleName);
            List<UserAdapter.Row> rows = new ArrayList<>();
            for (User u : users) {
                UserAdapter.Row row = new UserAdapter.Row();
                row.fullName = u.fullName;
                row.username = u.username;
                row.active = u.isActive;
                row.roleName = roleMap.get(u.roleId);
                if (row.roleName == null) row.roleName = "";
                rows.add(row);
            }
            handler.post(() -> adapter.submit(rows));
        });
    }
}
