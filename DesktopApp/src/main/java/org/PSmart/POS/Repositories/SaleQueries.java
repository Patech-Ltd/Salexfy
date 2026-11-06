package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.Query;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;

import java.util.*;

/**
 * Read/void operations for the Sales page. Every call opens its own short-lived EntityManager and
 * closes it (no shared sessions, no leaked connections), and the list screen loads lightweight
 * rows via a single projection query instead of whole Sale graphs.
 */
public final class SaleQueries {

    public static final List<String> VOID_STATUSES = List.of("VOID", "VOIDED", "CANCELLED", "CANCELED");

    private SaleQueries() {}

    public static final class Filter {
        public Long from, to;
        public String keyword = "";
        public String status = "All";      // All | Completed | Credit | Voided
        public String payment = "All";     // All | Cash | M-Pesa | Card | Credit
        public UUID cashierId;
    }

    public static final class Row {
        public UUID uid;
        public long createdAt;
        public String receipt, customer, cashier, payment, status;
        public int items;
        public double subtotal, vat, total;
        public boolean voided() { return status != null && VOID_STATUSES.contains(status.toUpperCase()); }
    }

    public static String receiptOf(UUID uid) {
        return "RCP-" + String.format("%06d", uid.hashCode() & 0xFFFFFF);
    }

    private static EntityManager em() {
        return DatabaseUtils.getInstance().getEmf().createEntityManager();
    }

    // ------------------------------------------------------------------ list

    private static String where(Filter f, Set<UUID> receiptIds, Map<String, Object> params) {
        StringBuilder w = new StringBuilder("s.deleted=false");
        if (f.from != null) { w.append(" AND s.createdAt >= :from"); params.put("from", f.from); }
        if (f.to != null) { w.append(" AND s.createdAt <= :to"); params.put("to", f.to); }
        switch (f.status) {
            case "Completed": w.append(" AND UPPER(s.status)='COMPLETED'"); break;
            case "Credit": w.append(" AND UPPER(s.status)='CREDIT'"); break;
            case "Voided": w.append(" AND UPPER(s.status) IN :bad"); params.put("bad", VOID_STATUSES); break;
            default: break;
        }
        if (!"All".equals(f.payment)) {
            String tag = f.payment.equals("M-Pesa") ? "MPESA" : f.payment.toUpperCase();
            w.append(" AND UPPER(s.paymentMethod) LIKE :pay");
            params.put("pay", "%" + tag + "%");
        }
        if (f.cashierId != null) { w.append(" AND s.cashier.uid = :cashier"); params.put("cashier", f.cashierId); }
        String k = f.keyword == null ? "" : f.keyword.trim().toLowerCase();
        if (!k.isEmpty()) {
            w.append(" AND (LOWER(c.fullname) LIKE :kw OR LOWER(s.paymentMethod) LIKE :kw");
            params.put("kw", "%" + k + "%");
            if (receiptIds != null && !receiptIds.isEmpty()) {
                w.append(" OR s.uid IN :rids");
                params.put("rids", receiptIds);
            }
            w.append(")");
        }
        return w.toString();
    }

    /** Receipt numbers are derived from the uid, so they cannot be searched in SQL: resolve them here. */
    private static Set<UUID> matchReceipts(EntityManager em, String keyword) {
        String k = keyword == null ? "" : keyword.trim().toUpperCase().replace("RCP-", "").replace("RCP", "");
        if (k.isEmpty() || !k.matches("\\d{1,8}")) return Collections.emptySet();
        Set<UUID> hits = new HashSet<>();
        for (Object o : em.createQuery("SELECT s.uid FROM Sale s WHERE s.deleted=false").getResultList()) {
            UUID id = (UUID) o;
            String num = String.format("%06d", id.hashCode() & 0xFFFFFF);
            if (num.contains(k)) hits.add(id);
        }
        return hits;
    }

    private static void bind(Query q, Map<String, Object> params) {
        for (Map.Entry<String, Object> e : params.entrySet()) q.setParameter(e.getKey(), e.getValue());
    }

    public static List<Row> rows(Filter f, int page, int size) {
        EntityManager em = em();
        try {
            Map<String, Object> params = new HashMap<>();
            String w = where(f, matchReceipts(em, f.keyword), params);
            Query q = em.createQuery("SELECT s.uid, s.createdAt, c.fullname, u.fullname, s.total, s.subtotal, s.vatAmount, "
                    + "s.paymentMethod, s.status, (SELECT COUNT(i) FROM SaleItem i WHERE i.sale = s AND i.deleted = false) "
                    + "FROM Sale s LEFT JOIN s.customer c LEFT JOIN s.cashier u WHERE " + w + " ORDER BY s.createdAt DESC");
            bind(q, params);
            q.setFirstResult(Math.max(0, page) * size);
            q.setMaxResults(size);
            List<Row> out = new ArrayList<>();
            for (Object o : q.getResultList()) {
                Object[] r = (Object[]) o;
                Row row = new Row();
                row.uid = (UUID) r[0];
                row.createdAt = r[1] == null ? 0 : ((Number) r[1]).longValue();
                row.customer = r[2] == null ? "Walk-in" : (String) r[2];
                row.cashier = r[3] == null ? "" : (String) r[3];
                row.total = num(r[4]);
                row.subtotal = num(r[5]);
                row.vat = num(r[6]);
                row.payment = r[7] == null ? "" : (String) r[7];
                row.status = r[8] == null ? "" : (String) r[8];
                row.items = r[9] == null ? 0 : ((Number) r[9]).intValue();
                row.receipt = receiptOf(row.uid);
                out.add(row);
            }
            return out;
        } finally {
            em.close();
        }
    }

    /** {count of all matching, count excluding voided, sum excluding voided}. */
    public static double[] summary(Filter f) {
        EntityManager em = em();
        try {
            Map<String, Object> params = new HashMap<>();
            String w = where(f, matchReceipts(em, f.keyword), params);
            Query all = em.createQuery("SELECT COUNT(s) FROM Sale s LEFT JOIN s.customer c WHERE " + w);
            bind(all, params);
            long count = ((Number) all.getSingleResult()).longValue();
            Map<String, Object> p2 = new HashMap<>(params);
            p2.put("badSum", VOID_STATUSES);
            Query live = em.createQuery("SELECT COUNT(s), COALESCE(SUM(s.total),0) FROM Sale s LEFT JOIN s.customer c WHERE "
                    + w + " AND (s.status IS NULL OR UPPER(s.status) NOT IN :badSum)");
            bind(live, p2);
            Object[] r = (Object[]) live.getSingleResult();
            return new double[]{count, ((Number) r[0]).doubleValue(), num(r[1])};
        } finally {
            em.close();
        }
    }

    public static Map<UUID, String> cashiers() {
        EntityManager em = em();
        try {
            Map<UUID, String> m = new LinkedHashMap<>();
            for (Object o : em.createQuery("SELECT DISTINCT u.uid, u.fullname FROM Sale s JOIN s.cashier u "
                    + "WHERE s.deleted=false ORDER BY u.fullname").getResultList()) {
                Object[] r = (Object[]) o;
                m.put((UUID) r[0], r[1] == null ? "(unnamed)" : (String) r[1]);
            }
            return m;
        } finally {
            em.close();
        }
    }

    // ------------------------------------------------------------------ detail

    /**
     * Read-only copy of a sale for viewing and printing, assembled from plain columns (no product
     * entities are loaded, so nothing can fail lazily or hold a connection). Do not persist it.
     */
    public static Sale load(UUID id) {
        EntityManager em = em();
        try {
            List<?> head = em.createQuery("SELECT s.createdAt, s.subtotal, s.discount, s.vatAmount, s.total, s.amountPaid, s.change, "
                    + "s.paymentMethod, s.status, s.voidedAt, s.voidReason, s.voidedByName, "
                    + "c.fullname, c.phone, c.email, c.address, c.currentBalance, u.fullname, u.username "
                    + "FROM Sale s LEFT JOIN s.customer c LEFT JOIN s.cashier u WHERE s.uid=:id")
                    .setParameter("id", id).getResultList();
            if (head.isEmpty()) return null;
            Object[] r = (Object[]) head.get(0);
            Sale s = Sale.builder().uid(id).createdAt(r[0] == null ? null : ((Number) r[0]).longValue())
                    .subtotal(dbl(r[1])).discount(dbl(r[2])).vatAmount(dbl(r[3])).total(dbl(r[4])).amountPaid(dbl(r[5]))
                    .change(dbl(r[6])).paymentMethod((String) r[7]).status((String) r[8])
                    .voidedAt(r[9] == null ? null : ((Number) r[9]).longValue()).voidReason((String) r[10]).voidedByName((String) r[11]).build();
            if (r[12] != null) {
                s.setCustomer(Customer.builder().fullname((String) r[12]).phone((String) r[13]).email((String) r[14])
                        .address((String) r[15]).currentBalance(dbl(r[16])).build());
            }
            if (r[17] != null || r[18] != null) {
                User u = new User();
                u.setFullname((String) r[17]);
                u.setUsername((String) r[18]);
                s.setCashier(u);
            }
            List<SaleItem> items = new ArrayList<>();
            for (Object o : em.createQuery("SELECT it.name, i.quantity, i.unitPrice, i.buyingPrice, i.discount, i.total "
                    + "FROM SaleItem i LEFT JOIN i.item it WHERE i.sale.uid=:id AND i.deleted=false")
                    .setParameter("id", id).getResultList()) {
                Object[] x = (Object[]) o;
                items.add(SaleItem.builder().item(Item.builder().name(x[0] == null ? "(item)" : (String) x[0]).build())
                        .quantity(dbl(x[1])).unitPrice(dbl(x[2])).buyingPrice(dbl(x[3])).discount(dbl(x[4])).total(dbl(x[5])).build());
            }
            s.setItems(items);
            return s;
        } finally {
            em.close();
        }
    }

    private static Double dbl(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }

    // ------------------------------------------------------------------ void

    /**
     * Cancels a sale in one transaction. Stock returns automatically (voided sales are excluded from
     * on-hand and from all reports); a credit charge is reversed on the customer's account and
     * loyalty points are put back. The sale stays on record with who/when/why.
     *
     * @return null on success, otherwise a message to show the user.
     */
    public static String voidSale(UUID id, String reason, User by) {
        return voidSale(id, reason, by, null);
    }

    /** @param approver the supervisor who approved it when {@code by} was not allowed to (recorded on the sale). */
    public static String voidSale(UUID id, String reason, User by, User approver) {
        if (reason == null || reason.trim().length() < 3) return "Enter a reason for cancelling this sale.";
        EntityManager em = em();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            List<?> head = em.createQuery("SELECT s.status, s.total, s.amountPaid, c.uid, s.pointsEarned, s.pointsRedeemed "
                    + "FROM Sale s LEFT JOIN s.customer c WHERE s.uid=:id").setParameter("id", id).getResultList();
            if (head.isEmpty()) { tx.rollback(); return "Sale not found."; }
            Object[] h = (Object[]) head.get(0);
            String status = (String) h[0];
            double total = num(h[1]);
            double paid = Math.min(num(h[2]), total);
            UUID customerId = (UUID) h[3];
            double earned = num(h[4]), redeemed = num(h[5]);
            boolean wasCredit = "CREDIT".equalsIgnoreCase(status);
            String who = org.PSmart.POS.Utils.SupervisorApproval.credit(by, approver);

            // Atomic flip: only one caller can cancel a given sale.
            int changed = em.createQuery("UPDATE Sale s SET s.status='VOIDED', s.voidedAt=:at, s.voidReason=:why, s.voidedByName=:who "
                    + "WHERE s.uid=:id AND (s.status IS NULL OR UPPER(s.status) NOT IN :bad)")
                    .setParameter("at", System.currentTimeMillis()).setParameter("why", reason.trim()).setParameter("who", who)
                    .setParameter("id", id).setParameter("bad", VOID_STATUSES).executeUpdate();
            if (changed == 0) { tx.rollback(); return "This sale is already cancelled."; }

            if (customerId != null) {
                Customer c = em.find(Customer.class, customerId);
                if (c != null) {
                    double bal = c.getCurrentBalance() == null ? 0 : c.getCurrentBalance();
                    double charge = wasCredit ? Math.max(0, total - paid) : 0;
                    if (charge > 0.004) {
                        double after = Math.max(0, bal - charge);
                        c.setCurrentBalance(after);
                        Sale ref = em.getReference(Sale.class, id);
                        em.persist(CreditTransaction.builder().customer(c).sale(ref).type("VOID").amount(charge)
                                .balanceAfter(after).createdAt(System.currentTimeMillis())
                                .notes("Sale " + receiptOf(id) + " cancelled: " + reason.trim()).build());
                    }
                    double pts = c.getLoyaltyPoints() == null ? 0 : c.getLoyaltyPoints();
                    if (earned > 0 || redeemed > 0) c.setLoyaltyPoints(Math.max(0, pts - earned + redeemed));
                    double spent = c.getTotalSpent() == null ? 0 : c.getTotalSpent();
                    double paidPart = total - (wasCredit ? Math.max(0, total - paid) : 0);
                    if (spent > 0) c.setTotalSpent(Math.max(0, spent - Math.max(0, paidPart)));
                }
            }
            tx.commit();
            return null;
        } catch (Exception e) {
            if (tx.isActive()) tx.rollback();
            Throwable t = e;
            while (t.getCause() != null) t = t.getCause();
            return "Could not cancel the sale: " + t.getMessage();
        } finally {
            em.close();
        }
    }

    private static double num(Object o) {
        return o instanceof Number ? ((Number) o).doubleValue() : 0;
    }
}
