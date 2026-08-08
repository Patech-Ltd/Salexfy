package com.patechltd.salexfypos.security;

import android.content.Context;
import android.content.SharedPreferences;

import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.Authority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Permission checker that NEVER touches the database from the main thread.
 * A snapshot of the current user's authorities is kept in memory and mirrored
 * to SharedPreferences, so {@link #has(Context, Authority)} is a fast read.
 * The snapshot is refreshed on a background thread after login and whenever
 * {@link #invalidate()} is called.
 */
public class PermissionChecker {

    private static final String PREFS = "salexfy_permissions";
    private static final String KEY_ROLE_ID = "role_id";
    private static final String KEY_ROLE_NAME = "role_name";
    private static final String KEY_AUTHORITIES = "authorities";

    private static volatile Set<String> cachedAuthorities;
    private static volatile String cachedRoleId;
    private static volatile String cachedRoleName;

    private PermissionChecker() {
    }

    /** Clears the in-memory cache and refreshes it from the DB on a background thread. */
    public static void invalidate() {
        cachedAuthorities = null;
        cachedRoleId = null;
        cachedRoleName = null;
    }

    /** Called after login: loads the role snapshot on a background thread and caches it. */
    public static void loadAsync(final Context context, final String userId) {
        if (userId == null) return;
        Repository.get(context).io(() -> {
            User user = Repository.get(context).admin.getUser(userId);
            if (user == null) return null;
            Role role = Repository.get(context).admin.getRole(user.roleId);
            Set<String> set = new HashSet<>();
            String roleName = "";
            String roleId = user.roleId;
            if (role != null) {
                set = RoleAuthorities.nameSet(RoleAuthorities.fromJson(role.authoritiesJson));
                roleName = role.roleName;
            }
            return new Snapshot(roleId, roleName, set);
        }).thenAccept(snapshot -> {
            if (snapshot == null) return;
            cachedRoleId = snapshot.roleId;
            cachedRoleName = snapshot.roleName;
            cachedAuthorities = snapshot.authorities;
            persist(context, snapshot);
        });
    }

    private static class Snapshot {
        final String roleId;
        final String roleName;
        final Set<String> authorities;

        Snapshot(String roleId, String roleName, Set<String> authorities) {
            this.roleId = roleId;
            this.roleName = roleName;
            this.authorities = authorities;
        }
    }

    private static void persist(Context context, Snapshot snapshot) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_ROLE_ID, snapshot.roleId)
                .putString(KEY_ROLE_NAME, snapshot.roleName)
                .putString(KEY_AUTHORITIES, String.join(",", snapshot.authorities))
                .apply();
    }

    private static void ensureLoaded(Context context) {
        if (cachedAuthorities != null) return;
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        cachedRoleId = prefs.getString(KEY_ROLE_ID, null);
        cachedRoleName = prefs.getString(KEY_ROLE_NAME, "");
        String joined = prefs.getString(KEY_AUTHORITIES, null);
        if (joined != null && !joined.isEmpty()) {
            Set<String> set = new HashSet<>();
            for (String a : joined.split(",")) {
                if (!a.isEmpty()) set.add(a);
            }
            cachedAuthorities = set;
        } else {
            cachedAuthorities = new HashSet<>();
        }
    }

    /** Fast, main-thread safe. Never blocks on the database. */
    public static boolean has(Context context, Authority authority) {
        ensureLoaded(context);
        return cachedAuthorities.contains(authority.name());
    }

    /** Fast, main-thread safe. */
    public static boolean isAdmin(Context context) {
        ensureLoaded(context);
        return cachedRoleName != null && "Administrator".equalsIgnoreCase(cachedRoleName);
    }

    /** Loads authorities for a specific role from the DB. Call from a background thread only. */
    public static List<Authority> authoritiesOf(Context context, String roleId) {
        Role role = Repository.get(context).admin.getRole(roleId);
        if (role == null) return new ArrayList<>();
        return RoleAuthorities.fromJson(role.authoritiesJson);
    }
}
