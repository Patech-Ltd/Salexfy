package org.PSmart.POS.Utils;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.StoreConfig;

import java.io.File;
import java.util.*;

/**
 * Everything the shop owner should know about, computed from the database: stock, expiry, customers
 * who owe, suppliers owed, backups and security. Used by the Notifications page and the menu badge.
 */
public final class AlertService {

    public enum Level { CRITICAL, WARNING, INFO, OK }

    /** Where the "Open" button should take the user (index of the dashboard tab), or -1. */
    public static final int TAB_NONE = -1, TAB_STOCK = 1, TAB_SALES = 2, TAB_CUSTOMERS = 4, TAB_SUPPLIERS = 5, TAB_ADMIN = 7, TAB_SETTINGS = 8;

    public static final class Alert {
        public final Level level;
        public final String category, title, detail, actionLabel;
        public final int tab;
        public final boolean backupNow;

        Alert(Level level, String category, String title, String detail, String actionLabel, int tab, boolean backupNow) {
            this.level = level;
            this.category = category;
            this.title = title;
            this.detail = detail;
            this.actionLabel = actionLabel;
            this.tab = tab;
            this.backupNow = backupNow;
        }
    }

    private AlertService() {}

    private static final long DAY = 24L * 60 * 60 * 1000;

    private static String list(List<String> names, int max) {
        if (names.size() <= max) return String.join(", ", names);
        return String.join(", ", names.subList(0, max)) + " and " + (names.size() - max) + " more";
    }

    private static String ago(long ms) {
        long mins = ms / 60000, hours = mins / 60, days = hours / 24;
        if (days >= 1) return days + " day" + (days == 1 ? "" : "s") + " ago";
        if (hours >= 1) return hours + " hour" + (hours == 1 ? "" : "s") + " ago";
        return Math.max(1, mins) + " min ago";
    }

    /** Never throws: a failing check is reported as a warning instead. */
    public static List<Alert> compute() {
        List<Alert> out = new ArrayList<>();
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        try {
            safe(out, "Stock", () -> stock(em, out));
            safe(out, "Expiry", () -> expiry(em, out));
            safe(out, "Customers", () -> customers(em, out));
            safe(out, "Suppliers", () -> suppliers(em, out));
            safe(out, "Backup", () -> backup(out));
            safe(out, "Security", () -> security(em, out));
            safe(out, "Sales", () -> sales(em, out));
        } finally {
            em.close();
        }
        boolean attention = out.stream().anyMatch(a -> a.level == Level.CRITICAL || a.level == Level.WARNING);
        if (!attention) out.add(0, new Alert(Level.OK, "All clear", "Everything looks healthy",
                "No stock, backup or security problems to deal with right now.", null, TAB_NONE, false));
        out.sort(Comparator.comparingInt(a -> a.level.ordinal()));
        return out;
    }

    /** Number of items needing action (critical + warning). */
    public static int attentionCount(List<Alert> alerts) {
        int n = 0;
        for (Alert a : alerts) if (a.level == Level.CRITICAL || a.level == Level.WARNING) n++;
        return n;
    }

    private interface Check { void run() throws Exception; }

    private static void safe(List<Alert> out, String name, Check c) {
        try {
            c.run();
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            out.add(new Alert(Level.WARNING, name, "Could not check " + name.toLowerCase(), String.valueOf(t.getMessage()), null, TAB_NONE, false));
        }
    }

    // ---------------------------------------------------------------- checks

    private static void stock(EntityManager em, List<Alert> out) {
        Map<UUID, Double> onHand = StockService.onHandAll(em);
        List<String> outNames = new ArrayList<>(), lowNames = new ArrayList<>();
        for (Object o : em.createQuery("SELECT i.uid, i.name, i.reorderLevel FROM Item i WHERE i.deleted=false ORDER BY i.name").getResultList()) {
            Object[] r = (Object[]) o;
            double oh = onHand.getOrDefault((UUID) r[0], 0.0);
            double reorder = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
            if (oh <= 0) outNames.add(String.valueOf(r[1]));
            else if (reorder > 0 && oh < reorder) lowNames.add(String.valueOf(r[1]) + " (" + (oh == Math.floor(oh) ? String.format("%.0f", oh) : String.format("%.1f", oh)) + " left)");
        }
        if (!outNames.isEmpty()) {
            out.add(new Alert(Level.CRITICAL, "Stock", outNames.size() + " item" + (outNames.size() == 1 ? " is" : "s are") + " out of stock",
                    list(outNames, 6), "View stock", TAB_STOCK, false));
        }
        if (!lowNames.isEmpty()) {
            out.add(new Alert(Level.WARNING, "Stock", lowNames.size() + " item" + (lowNames.size() == 1 ? " is" : "s are") + " running low",
                    list(lowNames, 6), "View stock", TAB_STOCK, false));
        }
    }

    private static void expiry(EntityManager em, List<Alert> out) {
        long now = System.currentTimeMillis();
        Map<UUID, Double> onHand = StockService.onHandAll(em);
        Set<String> expired = new TreeSet<>(), soon = new TreeSet<>();
        for (Object o : em.createQuery("SELECT f.item.uid, f.item.name, f.expiry FROM StockFeedItem f "
                + "WHERE f.deleted=false AND f.expiry IS NOT NULL AND f.item.deleted=false").getResultList()) {
            Object[] r = (Object[]) o;
            if (onHand.getOrDefault((UUID) r[0], 0.0) <= 0) continue;     // nothing left to expire
            long exp = ((Number) r[2]).longValue();
            if (exp < now) expired.add(String.valueOf(r[1]));
            else if (exp <= now + 30 * DAY) soon.add(String.valueOf(r[1]) + " (" + Math.max(0, (exp - now) / DAY) + " days)");
        }
        if (!expired.isEmpty()) out.add(new Alert(Level.CRITICAL, "Expiry", expired.size() + " product" + (expired.size() == 1 ? " has" : "s have") + " expired stock",
                list(new ArrayList<>(expired), 6) + ". Take it off the shelves.", "View stock", TAB_STOCK, false));
        if (!soon.isEmpty()) out.add(new Alert(Level.WARNING, "Expiry", soon.size() + " product" + (soon.size() == 1 ? " expires" : "s expire") + " within 30 days",
                list(new ArrayList<>(soon), 6), "View stock", TAB_STOCK, false));
    }

    private static void customers(EntityManager em, List<Alert> out) {
        double total = 0;
        int debtors = 0;
        List<String> over = new ArrayList<>();
        for (Object o : em.createQuery("SELECT c.fullname, c.currentBalance, c.creditLimit FROM Customer c WHERE c.deleted=false AND c.currentBalance > 0").getResultList()) {
            Object[] r = (Object[]) o;
            double bal = ((Number) r[1]).doubleValue();
            double lim = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
            total += bal;
            debtors++;
            if (lim > 0 && bal > lim + 0.004) over.add(r[0] + " (owes " + String.format("%,.0f", bal) + ", limit " + String.format("%,.0f", lim) + ")");
        }
        if (!over.isEmpty()) out.add(new Alert(Level.WARNING, "Customers", over.size() + " customer" + (over.size() == 1 ? " is" : "s are") + " over their credit limit",
                list(over, 4), "View customers", TAB_CUSTOMERS, false));
        if (debtors > 0) out.add(new Alert(Level.INFO, "Customers", "Customers owe you KES " + String.format("%,.2f", total),
                debtors + " customer" + (debtors == 1 ? "" : "s") + " have an unpaid balance.", "View customers", TAB_CUSTOMERS, false));
    }

    private static void suppliers(EntityManager em, List<Alert> out) {
        Map<String, Double> owed = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Object o : em.createQuery("SELECT p.supplierName, COALESCE(SUM(p.total - COALESCE(p.paidAmount, p.total)),0) FROM Purchase p "
                + "WHERE p.deleted=false AND p.supplierName IS NOT NULL GROUP BY p.supplierName").getResultList()) {
            Object[] r = (Object[]) o;
            owed.merge(String.valueOf(r[0]).trim(), ((Number) r[1]).doubleValue(), Double::sum);
        }
        for (Object o : em.createQuery("SELECT sp.supplierName, COALESCE(SUM(sp.amount),0) FROM SupplierPayment sp WHERE sp.deleted=false GROUP BY sp.supplierName").getResultList()) {
            Object[] r = (Object[]) o;
            owed.merge(String.valueOf(r[0]).trim(), -((Number) r[1]).doubleValue(), Double::sum);
        }
        double total = 0;
        List<String> names = new ArrayList<>();
        for (Map.Entry<String, Double> e : owed.entrySet()) {
            if (e.getValue() > 0.004) { total += e.getValue(); names.add(e.getKey() + " (" + String.format("%,.0f", e.getValue()) + ")"); }
        }
        if (!names.isEmpty()) out.add(new Alert(Level.INFO, "Suppliers", "You owe suppliers KES " + String.format("%,.2f", total), list(names, 4),
                "View suppliers", TAB_SUPPLIERS, false));
    }

    private static void backup(List<Alert> out) {
        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        File latest = BackupService.latestBackup();
        long now = System.currentTimeMillis();
        long last = latest != null ? latest.lastModified() : (cfg != null && cfg.getLastBackupAt() != null ? cfg.getLastBackupAt() : 0);
        boolean auto = cfg != null && Boolean.TRUE.equals(cfg.getAutoBackupEnabled());
        if (last == 0) {
            out.add(new Alert(Level.CRITICAL, "Backup", "No backup has ever been made",
                    "If this computer fails you could lose all sales and stock records.", "Back up now", TAB_NONE, true));
        } else if (now - last > 7 * DAY) {
            out.add(new Alert(Level.CRITICAL, "Backup", "Your last backup is " + ago(now - last),
                    "Back up now, and turn on automatic backups in Settings.", "Back up now", TAB_NONE, true));
        } else if (now - last > 2 * DAY) {
            out.add(new Alert(Level.WARNING, "Backup", "Your last backup is " + ago(now - last), "A fresh backup is due.", "Back up now", TAB_NONE, true));
        } else {
            out.add(new Alert(Level.INFO, "Backup", "Backed up " + ago(now - last), BackupService.listBackups().size() + " copies kept in "
                    + new File(BackupService.BACKUP_DIR).getAbsolutePath(), null, TAB_NONE, false));
        }
        if (!auto) out.add(new Alert(Level.WARNING, "Backup", "Automatic backup is switched off",
                "Turn it on under Settings > Backup & sync so you never depend on remembering.", "Open settings", TAB_SETTINGS, false));
    }

    private static void security(EntityManager em, List<Alert> out) {
        List<String> weak = new ArrayList<>();
        int plain = 0, noRole = 0;
        for (Object o : em.createQuery("SELECT u.uid, u.username, u.password FROM User u WHERE u.deleted=false").getResultList()) {
            Object[] r = (Object[]) o;
            String pw = r[2] == null ? "" : String.valueOf(r[2]);
            if (!PasswordUtil.isHashed(pw)) {
                plain++;
                String l = pw.toLowerCase();
                if (pw.length() < 6 || l.equals("admin123") || l.equals("password") || l.equals("123456") || l.equals("1234")) weak.add(String.valueOf(r[1]));
            }
            long roles = em.createQuery("SELECT COUNT(x) FROM Role x WHERE x.user.uid=:u", Long.class).setParameter("u", r[0]).getSingleResult();
            if (roles == 0) noRole++;
        }
        if (!weak.isEmpty()) out.add(new Alert(Level.CRITICAL, "Security", "Weak password in use: " + list(weak, 5),
                "Change it now. Anyone who guesses it can see and change your shop's data.", "Open users", TAB_ADMIN, false));
        if (plain > 0) out.add(new Alert(Level.WARNING, "Security", plain + " account" + (plain == 1 ? " still uses" : "s still use") + " an old unprotected password",
                "It is upgraded to a protected password the next time that person signs in.", "Open users", TAB_ADMIN, false));
        if (noRole > 0) out.add(new Alert(Level.WARNING, "Security", noRole + " user" + (noRole == 1 ? " has" : "s have") + " no role assigned",
                "People with no role can open every page. Give each person a role.", "Open users", TAB_ADMIN, false));
        long admins = em.createQuery("SELECT COUNT(DISTINCT r.user.uid) FROM Role r WHERE r.roleType=:t AND r.change=true AND r.user.deleted=false", Long.class)
                .setParameter("t", org.PSmart.POS.Entities.RoleType.ADMIN).getSingleResult();
        if (admins == 0 && noRole == 0) out.add(new Alert(Level.CRITICAL, "Security", "Nobody is an administrator",
                "Give one trusted person the Owner role.", "Open users", TAB_ADMIN, false));
        long week = System.currentTimeMillis() - 7 * DAY;
        long voided = em.createQuery("SELECT COUNT(s) FROM Sale s WHERE s.deleted=false AND UPPER(s.status) IN :bad AND s.voidedAt >= :w", Long.class)
                .setParameter("bad", org.PSmart.POS.Repositories.SaleQueries.VOID_STATUSES).setParameter("w", week).getSingleResult();
        if (voided >= 5) out.add(new Alert(Level.WARNING, "Security", voided + " sales were cancelled in the last 7 days",
                "Review who cancelled them and why on the Sales page (filter by status: Voided).", "View sales", TAB_SALES, false));
        out.add(new Alert(Level.INFO, "Security", "Database login is kept in the application's configuration",
                "Keep this computer locked, and restrict who can copy the program folder.", null, TAB_NONE, false));
    }

    private static void sales(EntityManager em, List<Alert> out) {
        long start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        Object[] r = (Object[]) em.createQuery("SELECT COUNT(s), COALESCE(SUM(s.total),0) FROM Sale s WHERE s.deleted=false "
                + "AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad) AND s.createdAt >= :st")
                .setParameter("bad", org.PSmart.POS.Repositories.SaleQueries.VOID_STATUSES).setParameter("st", start).getSingleResult();
        out.add(new Alert(Level.INFO, "Sales", "Today: KES " + String.format("%,.2f", ((Number) r[1]).doubleValue()),
                ((Number) r[0]).longValue() + " receipt(s) so far today.", "View sales", TAB_SALES, false));
        StoreConfig cfg = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
        if (cfg == null || cfg.getDefaultPrinter() == null || cfg.getDefaultPrinter().isBlank() || cfg.getDefaultPrinter().startsWith("(")) {
            out.add(new Alert(Level.INFO, "Setup", "No receipt printer chosen", "Receipts go to the computer's default printer. Pick one in Settings > Receipt & printer.",
                    "Open settings", TAB_SETTINGS, false));
        }
    }
}
