package com.patechltd.salexfypos;

import android.content.Context;

import com.patechltd.salexfypos.db.Repository;
import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Role;
import com.patechltd.salexfypos.db.entity.Unit;
import com.patechltd.salexfypos.db.entity.User;
import com.patechltd.salexfypos.model.Authority;
import com.patechltd.salexfypos.security.PasswordHasher;
import com.patechltd.salexfypos.security.RoleAuthorities;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SeedData {

    private SeedData() {
    }

    public static void seed(Context context) {
        Repository repo = Repository.get(context);
        long now = System.currentTimeMillis();

        List<Authority> adminAuths = new ArrayList<>();
        for (Authority a : Authority.values()) adminAuths.add(a);

        List<Authority> cashierAuths = new ArrayList<>();
        cashierAuths.add(Authority.DASHBOARD_VIEW);
        cashierAuths.add(Authority.SELL_VIEW);
        cashierAuths.add(Authority.SALE_HOLD);
        cashierAuths.add(Authority.PRODUCT_VIEW);
        cashierAuths.add(Authority.STOCK_VIEW);
        cashierAuths.add(Authority.CUSTOMER_VIEW);
        cashierAuths.add(Authority.CUSTOMER_PAYMENT);
        cashierAuths.add(Authority.REPORT_VIEW);
        cashierAuths.add(Authority.COMMISSION_VIEW);

        Role adminRole = new Role();
        adminRole.id = UUID.randomUUID().toString();
        adminRole.roleName = "Administrator";
        adminRole.authoritiesJson = RoleAuthorities.toJson(adminAuths);
        adminRole.commissionPercent = 0;
        adminRole.isDefault = true;
        adminRole.createdAt = now;
        repo.admin.insertRole(adminRole);

        Role cashierRole = new Role();
        cashierRole.id = UUID.randomUUID().toString();
        cashierRole.roleName = "Cashier";
        cashierRole.authoritiesJson = RoleAuthorities.toJson(cashierAuths);
        cashierRole.commissionPercent = 1;
        cashierRole.isDefault = false;
        cashierRole.createdAt = now;
        repo.admin.insertRole(cashierRole);

        Role managerRole = new Role();
        managerRole.id = UUID.randomUUID().toString();
        List<Authority> managerAuths = new ArrayList<>(cashierAuths);
        managerAuths.add(Authority.PRODUCT_EDIT);
        managerAuths.add(Authority.PURCHASE_VIEW);
        managerAuths.add(Authority.PURCHASE_EDIT);
        managerAuths.add(Authority.SUPPLIER_VIEW);
        managerAuths.add(Authority.SUPPLIER_EDIT);
        managerAuths.add(Authority.CUSTOMER_EDIT);
        managerAuths.add(Authority.STOCK_TAKE);
        managerRole.roleName = "Manager";
        managerRole.authoritiesJson = RoleAuthorities.toJson(managerAuths);
        managerRole.commissionPercent = 0.5;
        managerRole.isDefault = false;
        managerRole.createdAt = now;
        repo.admin.insertRole(managerRole);

        String salt = PasswordHasher.generateSalt();
        User admin = new User();
        admin.id = UUID.randomUUID().toString();
        admin.username = "admin";
        admin.passwordHash = PasswordHasher.hash("admin123", salt);
        admin.salt = salt;
        admin.fullName = "Administrator";
        admin.roleId = adminRole.id;
        admin.isActive = true;
        admin.createdAt = now;
        repo.admin.insertUser(admin);

        seedCategory(context, "General", 1, now);
        seedCategory(context, "Food & Drinks", 2, now);
        seedCategory(context, "Cleaning", 3, now);
        seedCategory(context, "Stationery", 4, now);

        seedBrand(context, "No Brand", now);

        seedUnit(context, "Piece", false, now);
        seedUnit(context, "Kilogram", false, now);
        seedUnit(context, "Litre", false, now);
        seedUnit(context, "Carton", true, now);
        seedUnit(context, "Dozen", true, now);
    }

    private static void seedCategory(Context context, String name, int order, long now) {
        Repository repo = Repository.get(context);
        if (repo.directory.categoryCount() > 0) return;
        Category c = new Category();
        c.id = UUID.randomUUID().toString();
        c.name = name;
        c.sortOrder = order;
        c.createdAt = now;
        repo.directory.insertCategory(c);
    }

    private static void seedBrand(Context context, String name, long now) {
        Repository repo = Repository.get(context);
        if (repo.directory.brandCount() > 0) return;
        Brand b = new Brand();
        b.id = UUID.randomUUID().toString();
        b.name = name;
        b.createdAt = now;
        repo.directory.insertBrand(b);
    }

    private static void seedUnit(Context context, String name, boolean wholesale, long now) {
        Repository repo = Repository.get(context);
        if (repo.directory.unitCount() > 0) return;
        Unit u = new Unit();
        u.id = UUID.randomUUID().toString();
        u.name = name;
        u.isWholesale = wholesale;
        u.createdAt = now;
        repo.directory.insertUnit(u);
    }
}
