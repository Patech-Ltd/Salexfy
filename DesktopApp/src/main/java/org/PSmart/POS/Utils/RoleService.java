package org.PSmart.POS.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.RoleProfile;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;

import java.util.*;

/** Role profiles stored in the database, and applying them to users. */
public final class RoleService {

    private RoleService() {}

    /** view/change flags per area. */
    public static Map<RoleType, boolean[]> decode(String text) {
        Map<RoleType, boolean[]> m = new EnumMap<>(RoleType.class);
        if (text == null || text.isBlank()) return m;
        for (String part : text.split(";")) {
            String[] kv = part.split("=");
            if (kv.length != 2) continue;
            try {
                RoleType t = RoleType.valueOf(kv[0].trim());
                String[] f = kv[1].split(",");
                m.put(t, new boolean[]{"1".equals(f[0].trim()), f.length > 1 && "1".equals(f[1].trim())});
            } catch (IllegalArgumentException ignored) {
                // unknown area from a newer version
            }
        }
        return m;
    }

    public static String encode(Map<RoleType, boolean[]> m) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<RoleType, boolean[]> e : m.entrySet()) {
            if (!e.getValue()[0] && !e.getValue()[1]) continue;
            if (sb.length() > 0) sb.append(';');
            sb.append(e.getKey().name()).append('=').append(e.getValue()[0] ? 1 : 0).append(',').append(e.getValue()[1] ? 1 : 0);
        }
        return sb.toString();
    }

    private static String all(boolean change) {
        Map<RoleType, boolean[]> m = new EnumMap<>(RoleType.class);
        for (RoleType t : RoleType.values()) m.put(t, new boolean[]{true, change});
        return encode(m);
    }

    private static String of(Object... pairs) {
        Map<RoleType, boolean[]> m = new EnumMap<>(RoleType.class);
        for (int i = 0; i < pairs.length; i += 3) m.put((RoleType) pairs[i], new boolean[]{(boolean) pairs[i + 1], (boolean) pairs[i + 2]});
        return encode(m);
    }

    /** Creates Owner / Manager / Cashier / Stock Keeper the first time. */
    public static void seedDefaults() {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            long n = em.createQuery("SELECT COUNT(r) FROM RoleProfile r WHERE r.deleted=false", Long.class).getSingleResult();
            if (n > 0) return;
            tx.begin();
            em.persist(RoleProfile.builder().name("Owner").builtIn(true)
                    .description("Full access to everything, including users and settings.").permissions(all(true)).build());
            em.persist(RoleProfile.builder().name("Manager").builtIn(true)
                    .description("Runs the shop: sales, stock, purchases, expenses and reports. Cannot manage users.")
                    .permissions(of(RoleType.CASHIER, true, true, RoleType.STOCK, true, true, RoleType.PURCHASE, true, true,
                            RoleType.EXPENSE, true, true, RoleType.REPORTS, true, true, RoleType.SETTINGS, true, false)).build());
            em.persist(RoleProfile.builder().name("Cashier").builtIn(true)
                    .description("Serves customers at the till only.").permissions(of(RoleType.CASHIER, true, true)).build());
            em.persist(RoleProfile.builder().name("Stock Keeper").builtIn(true)
                    .description("Receives and counts stock, records purchases.")
                    .permissions(of(RoleType.STOCK, true, true, RoleType.PURCHASE, true, true)).build());
            tx.commit();
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
        } finally {
            em.close();
        }
    }

    public static List<RoleProfile> profiles() {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            return em.createQuery("FROM RoleProfile r WHERE r.deleted=false ORDER BY r.name", RoleProfile.class).getResultList();
        } finally {
            em.close();
        }
    }

    public static Map<RoleType, boolean[]> permissionsOf(UUID userId) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            Map<RoleType, boolean[]> m = new EnumMap<>(RoleType.class);
            for (Role r : em.createQuery("FROM Role r WHERE r.user.uid=:u", Role.class).setParameter("u", userId).getResultList()) {
                if (r.getRoleType() != null) m.put(r.getRoleType(), new boolean[]{Boolean.TRUE.equals(r.getView()), Boolean.TRUE.equals(r.getChange())});
            }
            return m;
        } finally {
            em.close();
        }
    }

    /** Replaces the user's Role rows with the given permissions. */
    public static void applyPermissions(UUID userId, Map<RoleType, boolean[]> perms, String profileName, Double commissionRate) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            User u = em.find(User.class, userId);
            em.createQuery("DELETE FROM Role r WHERE r.user.uid=:u").setParameter("u", userId).executeUpdate();
            for (Map.Entry<RoleType, boolean[]> e : perms.entrySet()) {
                if (!e.getValue()[0] && !e.getValue()[1]) continue;
                Role r = new Role();
                r.setUser(u);
                r.setRoleType(e.getKey());
                r.setView(e.getValue()[0] || e.getValue()[1]);
                r.setChange(e.getValue()[1]);
                em.persist(r);
            }
            u.setRoleProfile(profileName);
            u.setCommissionRate(commissionRate);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }

    /** True when the user may open an area. Users with no roles at all keep full access (legacy installs). */
    public static boolean canView(User u, RoleType area) {
        if (u == null) return false;
        Map<RoleType, boolean[]> p = permissionsOf(u.getUid());
        if (p.isEmpty()) return true;
        boolean[] f = p.get(area);
        return f != null && (f[0] || f[1]);
    }

    public static boolean canChange(User u, RoleType area) {
        if (u == null) return false;
        Map<RoleType, boolean[]> p = permissionsOf(u.getUid());
        if (p.isEmpty()) return true;
        boolean[] a = p.get(RoleType.ADMIN);
        if (a != null && a[1]) return true;
        boolean[] f = p.get(area);
        return f != null && f[1];
    }
}
