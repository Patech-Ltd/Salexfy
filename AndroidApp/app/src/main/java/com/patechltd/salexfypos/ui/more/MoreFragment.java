package com.patechltd.salexfypos.ui.more;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.patechltd.salexfypos.MainActivity;
import com.patechltd.salexfypos.R;
import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PermissionChecker;
import com.patechltd.salexfypos.security.Session;
import com.patechltd.salexfypos.ui.help.HelpActivity;
import com.patechltd.salexfypos.ui.settings.DebugActivity;
import com.patechltd.salexfypos.ui.settings.SettingsActivity;
import com.patechltd.salexfypos.ui.suppliers.CustomerListActivity;
import com.patechltd.salexfypos.ui.suppliers.SupplierListActivity;
import com.patechltd.salexfypos.ui.users.UserListActivity;
import com.patechltd.salexfypos.util.DialogUtil;
import com.patechltd.salexfypos.util.Prefs;

public class MoreFragment extends Fragment {

    private Repository repo;
    private View rowUsers, rowSuppliers, rowCustomers, rowBackup, rowSettings, rowDebug;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_more, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = Repository.get(requireContext());

        rowUsers = view.findViewById(R.id.row_users);
        rowSuppliers = view.findViewById(R.id.row_suppliers);
        rowCustomers = view.findViewById(R.id.row_customers);
        rowBackup = view.findViewById(R.id.row_backup);
        rowSettings = view.findViewById(R.id.row_settings);
        rowDebug = view.findViewById(R.id.row_debug);

        rowUsers.setOnClickListener(v -> startActivity(new Intent(requireContext(), UserListActivity.class)));
        rowSuppliers.setOnClickListener(v -> startActivity(new Intent(requireContext(), SupplierListActivity.class)));
        rowCustomers.setOnClickListener(v -> startActivity(new Intent(requireContext(), CustomerListActivity.class)));
        rowBackup.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class).putExtra("openBackup", true)));
        rowSettings.setOnClickListener(v -> startActivity(new Intent(requireContext(), SettingsActivity.class)));
        rowDebug.setOnClickListener(v -> startActivity(new Intent(requireContext(), DebugActivity.class)));
        view.findViewById(R.id.row_help).setOnClickListener(v -> startActivity(new Intent(requireContext(), HelpActivity.class)));
        view.findViewById(R.id.btn_logout).setOnClickListener(v -> logout());
    }

    @Override
    public void onResume() {
        super.onResume();
        applyPermissions();
        loadHeader();
    }

    private void applyPermissions() {
        rowUsers.setVisibility(PermissionChecker.has(requireContext(), Authority.USER_VIEW) ? View.VISIBLE : View.GONE);
        rowSuppliers.setVisibility(PermissionChecker.has(requireContext(), Authority.SUPPLIER_VIEW) ? View.VISIBLE : View.GONE);
        rowCustomers.setVisibility(PermissionChecker.has(requireContext(), Authority.CUSTOMER_VIEW) ? View.VISIBLE : View.GONE);
        rowBackup.setVisibility(PermissionChecker.has(requireContext(), Authority.BACKUP_MANAGE) ? View.VISIBLE : View.GONE);
        rowSettings.setVisibility(PermissionChecker.has(requireContext(), Authority.SETTINGS_EDIT) ? View.VISIBLE : View.GONE);
        rowDebug.setVisibility(PermissionChecker.has(requireContext(), Authority.DEBUG_VIEW) ? View.VISIBLE : View.GONE);
    }

    private void loadHeader() {
        TextView name = getView() == null ? null : getView().findViewById(R.id.header_name);
        TextView role = getView() == null ? null : getView().findViewById(R.id.header_role);
        if (name == null) return;
        String userId = Session.userId(requireContext());
        repo.run(() -> {
            User user = userId == null ? null : repo.admin.getUser(userId);
            Role userRole = user == null ? null : repo.admin.getRole(user.roleId);
            String userName = user == null ? "Unknown" : user.fullName;
            String roleName = userRole == null ? "" : userRole.roleName;
            requireActivity().runOnUiThread(() -> {
                name.setText(userName);
                role.setText(roleName);
            });
        });
        String v = getVersion();
        TextView version = getView().findViewById(R.id.header_version);
        if (version != null) version.setText(v);

        TextView backupSub = getView().findViewById(R.id.backup_subtitle);
        if (backupSub != null) {
            long last = Prefs.getLong(requireContext(), Prefs.KEY_LAST_BACKUP, 0);
            backupSub.setText(last == 0 ? "Never backed up" : "Last backup " + com.patechltd.salexfypos.util.DateUtil.formatDate(last));
        }
    }

    private String getVersion() {
        try {
            return "v" + requireContext().getPackageManager()
                    .getPackageInfo(requireContext().getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "v1.0";
        }
    }

    private void logout() {
        DialogUtil.confirm(requireContext(), "Log out", "End this session?", () -> {
            PermissionChecker.invalidate();
            Session.end(requireContext());
            ((MainActivity) requireActivity()).finish();
        });
    }
}
