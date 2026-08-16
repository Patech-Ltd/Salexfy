package com.patechltd.salexfypos.helper;

import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.RoleAuthorities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoleHelper {

    private RoleHelper() {
    }

    public static Role adminRoleFor(Repository repo) {
        Role existing = repo.admin.findRoleByName("Administrator");
        if (existing != null) return existing;
        List<Authority> auths = new ArrayList<>();
        for (Authority a : Authority.values()) auths.add(a);
        Role role = new Role();
        role.uid = UUID.randomUUID().toString();
        role.roleName = "Administrator";
        role.authoritiesJson = RoleAuthorities.toJson(auths);
        role.isDefault = true;
        role.createdAt = System.currentTimeMillis();
        repo.admin.insertRole(role);
        return role;
    }
}
