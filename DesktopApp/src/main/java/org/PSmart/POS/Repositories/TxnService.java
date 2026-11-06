package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;
import org.PSmart.POS.Utils.StockService;

import java.util.*;
import java.util.function.Function;

/**
 * Edit / cancel for money and stock transactions other than sales (see SaleQueries.voidSale):
 * purchases, expenses, stock received, stock adjustments, supplier payments and customer payments.
 * Nothing is physically deleted: a cancelled record keeps who/when/why and simply stops counting
 * (deleted=true), so stock, supplier balances, customer balances and reports stay correct.
 * Every method returns null on success or a message to show the user.
 */
public final class TxnService {

    private TxnService() {}

    private static EntityManager em() { return DatabaseUtils.getInstance().getEmf().createEntityManager(); }

    private static String err(Throwable e) {
        Throwable t = e;
        while (t.getCause() != null) t = t.getCause();
        return "Could not save: " + t.getMessage();
    }

    /** Runs one unit of work in a transaction. */
    private static String tx(Function<EntityManager, String> work) {
        EntityManager em = em();
        EntityTransaction t = em.getTransaction();
        try {
            t.begin();
            String problem = work.apply(em);
            if (problem != null) { t.rollback(); return problem; }
            t.commit();
            return null;
        } catch (Exception e) {
            if (t.isActive()) t.rollback();
            return err(e);
        } finally {
            em.close();
        }
    }

    private static String trim(String s) { return s == null || s.trim().isEmpty() ? null : s.trim(); }

    // ------------------------------------------------------------------ expenses

    public static List<Object[]> expenseRows(boolean cancelled) {
        EntityManager em = em();
        try {
            List<Object[]> out = new ArrayList<>();
            for (Object o : em.createQuery("SELECT e.uid, e.createdAt, e.category, e.description, e.amount, e.paymentMethod, e.cancelReason "
                    + "FROM Expense e WHERE " + (cancelled ? "e.deleted=true AND e.cancelledAt IS NOT NULL" : "e.deleted=false")
                    + " ORDER BY e.createdAt DESC").getResultList()) out.add((Object[]) o);
            return out;
        } finally { em.close(); }
    }

    /** Plain copy of one expense (no entity graph). Index: see Expense fields. */
    public static Expense expense(UUID id) {
        EntityManager em = em();
        try {
            List<?> l = em.createQuery("SELECT e.createdAt, e.category, e.description, e.amount, e.paymentMethod, e.receiptNumber, e.notes, "
                    + "e.deleted, e.cancelledAt, e.cancelReason, e.cancelledBy, e.editedAt, e.editedBy, u.fullname, u.username "
                    + "FROM Expense e LEFT JOIN e.createdBy u WHERE e.uid=:id").setParameter("id", id).getResultList();
            if (l.isEmpty()) return null;
            Object[] r = (Object[]) l.get(0);
            Expense x = Expense.builder().uid(id).createdAt(lng(r[0])).category((String) r[1]).description((String) r[2])
                    .amount(dbl(r[3])).paymentMethod((String) r[4]).receiptNumber((String) r[5]).notes((String) r[6])
                    .deleted(Boolean.TRUE.equals(r[7])).cancelledAt(lng(r[8])).cancelReason((String) r[9]).cancelledBy((String) r[10])
                    .editedAt(lng(r[11])).editedBy((String) r[12]).build();
            if (r[13] != null || r[14] != null) {
                User u = new User();
                u.setFullname((String) r[13]);
                u.setUsername((String) r[14]);
                x.setCreatedBy(u);
            }
            return x;
        } finally { em.close(); }
    }

    public static String updateExpense(UUID id, String category, String description, double amount, String method,
                                       String receipt, String notes, long when, String by) {
        if (amount <= 0) return "Enter an amount greater than zero.";
        if (description == null || description.trim().isEmpty()) return "Enter a description.";
        return tx(em -> {
            Expense e = em.find(Expense.class, id);
            if (e == null || e.isDeleted()) return "This expense is no longer active, so it can't be edited.";
            e.setCategory(category);
            e.setDescription(description.trim());
            e.setAmount(amount);
            e.setPaymentMethod(method);
            e.setReceiptNumber(trim(receipt));
            e.setNotes(trim(notes));
            e.setCreatedAt(when);
            e.setEditedAt(System.currentTimeMillis());
            e.setEditedBy(by);
            return null;
        });
    }

    public static String cancelExpense(UUID id, String reason, String by) {
        return tx(em -> {
            Expense e = em.find(Expense.class, id);
            if (e == null) return "Expense not found.";
            if (e.isDeleted()) return "This expense is already cancelled.";
            e.setDeleted(true);
            e.setCancelledAt(System.currentTimeMillis());
            e.setCancelReason(reason);
            e.setCancelledBy(by);
            return null;
        });
    }

    // ------------------------------------------------------------------ purchases + stock received

    /** {cancelledAt, cancelReason, cancelledBy, editedAt, editedBy, feedUid, deleted}. */
    public static Object[] purchaseMeta(UUID id) {
        EntityManager em = em();
        try {
            List<?> l = em.createQuery("SELECT p.cancelledAt, p.cancelReason, p.cancelledBy, p.editedAt, p.editedBy, p.feedUid, p.deleted "
                    + "FROM Purchase p WHERE p.uid=:id").setParameter("id", id).getResultList();
            return l.isEmpty() ? null : (Object[]) l.get(0);
        } finally { em.close(); }
    }

    public static String updatePurchase(UUID id, String supplier, String invoice, String notes, double paid, String by) {
        return tx(em -> {
            Purchase p = em.find(Purchase.class, id);
            if (p == null || p.isDeleted()) return "This purchase is no longer active, so it can't be edited.";
            double total = p.getTotal() == null ? 0 : p.getTotal();
            if (paid < 0 || paid > total + 0.009) return "Amount paid must be between 0 and the total (" + String.format("%,.2f", total) + ").";
            p.setSupplierName(trim(supplier));
            p.setInvoiceNumber(trim(invoice));
            p.setNotes(trim(notes));
            p.setPaidAmount(paid);
            p.setEditedAt(System.currentTimeMillis());
            p.setEditedBy(by);
            if (p.getFeedUid() != null) {
                StockFeed f = em.find(StockFeed.class, p.getFeedUid());
                if (f != null) f.setComment("Purchase" + (p.getInvoiceNumber() == null ? "" : " " + p.getInvoiceNumber())
                        + (p.getSupplierName() == null ? "" : " from " + p.getSupplierName()));
            }
            return null;
        });
    }

    public static String cancelPurchase(UUID id, String reason, String by) {
        return tx(em -> {
            Purchase p = em.find(Purchase.class, id);
            if (p == null) return "Purchase not found.";
            if (p.isDeleted()) return "This purchase is already cancelled.";
            if (p.getFeedUid() != null) {
                String bad = cancelFeedIn(em, p.getFeedUid(), reason, by);
                if (bad != null) return bad;
            }
            stampPurchase(p, reason, by);
            return null;
        });
    }

    private static void stampPurchase(Purchase p, String reason, String by) {
        p.setDeleted(true);
        p.setStatus("CANCELLED");
        p.setCancelledAt(System.currentTimeMillis());
        p.setCancelReason(reason);
        p.setCancelledBy(by);
    }

    /** Stock received: header + lines. Row = {uid, doneOn, comment, lines, qty, cost, cancelReason}. */
    public static List<Object[]> feedRows(boolean cancelled) {
        EntityManager em = em();
        try {
            List<Object[]> out = new ArrayList<>();
            for (Object o : em.createQuery("SELECT f.uid, f.doneOn, f.comment, COUNT(i), COALESCE(SUM(i.quantity),0), "
                    + "COALESCE(SUM(i.quantity * i.buyingPrice),0), f.cancelReason FROM StockFeed f LEFT JOIN f.stockFeedItems i "
                    + "WHERE " + (cancelled ? "f.deleted=true AND f.cancelledAt IS NOT NULL" : "f.deleted=false")
                    + " GROUP BY f.uid, f.doneOn, f.comment, f.cancelReason ORDER BY f.doneOn DESC")
                    .setMaxResults(500).getResultList()) out.add((Object[]) o);
            return out;
        } finally { em.close(); }
    }

    /** {doneOn, comment, deleted, cancelledAt, cancelReason, cancelledBy, editedAt, editedBy}. */
    public static Object[] feedMeta(UUID id) {
        EntityManager em = em();
        try {
            List<?> l = em.createQuery("SELECT f.doneOn, f.comment, f.deleted, f.cancelledAt, f.cancelReason, f.cancelledBy, f.editedAt, f.editedBy "
                    + "FROM StockFeed f WHERE f.uid=:id").setParameter("id", id).getResultList();
            return l.isEmpty() ? null : (Object[]) l.get(0);
        } finally { em.close(); }
    }

    /** Lines of a delivery: {itemName, qty, buyingPrice, sellingPrice, expiry}. */
    public static List<Object[]> feedLines(UUID id) {
        EntityManager em = em();
        try {
            List<Object[]> out = new ArrayList<>();
            for (Object o : em.createQuery("SELECT it.name, i.quantity, i.buyingPrice, i.sellingPrice, i.expiry FROM StockFeed f "
                    + "JOIN f.stockFeedItems i LEFT JOIN i.item it WHERE f.uid=:id").setParameter("id", id).getResultList())
                out.add((Object[]) o);
            return out;
        } finally { em.close(); }
    }

    /** The purchase that booked this delivery, or null: {uid, supplier, total, paid}. */
    public static Object[] purchaseOfFeed(UUID feedId) {
        EntityManager em = em();
        try {
            List<?> l = em.createQuery("SELECT p.uid, p.supplierName, p.total, p.paidAmount FROM Purchase p WHERE p.feedUid=:id")
                    .setParameter("id", feedId).getResultList();
            return l.isEmpty() ? null : (Object[]) l.get(0);
        } finally { em.close(); }
    }

    public static String updateFeedNote(UUID id, String note, String by) {
        return tx(em -> {
            StockFeed f = em.find(StockFeed.class, id);
            if (f == null || f.isDeleted()) return "This delivery is no longer active, so it can't be edited.";
            f.setComment(trim(note));
            f.setEditedAt(System.currentTimeMillis());
            f.setEditedBy(by);
            return null;
        });
    }

    public static String cancelFeed(UUID id, String reason, String by) {
        return tx(em -> {
            String bad = cancelFeedIn(em, id, reason, by);
            if (bad != null) return bad;
            for (Purchase p : em.createQuery("FROM Purchase p WHERE p.feedUid=:id AND p.deleted=false", Purchase.class)
                    .setParameter("id", id).getResultList()) stampPurchase(p, reason, by);
            return null;
        });
    }

    /** Takes a delivery back out of stock, refusing if that stock has already been sold. */
    private static String cancelFeedIn(EntityManager em, UUID feedId, String reason, String by) {
        StockFeed f = em.find(StockFeed.class, feedId);
        if (f == null) return null;
        if (f.isDeleted()) return null;
        List<StockFeedItem> lines = em.createQuery("SELECT i FROM StockFeed f JOIN f.stockFeedItems i WHERE f.uid=:id", StockFeedItem.class)
                .setParameter("id", feedId).getResultList();
        Map<UUID, Double> have = StockService.onHandAll(em);
        Map<UUID, Double> take = new LinkedHashMap<>();
        Map<UUID, String> names = new HashMap<>();
        for (StockFeedItem i : lines) {
            if (i.isDeleted() || i.getItem() == null) continue;
            take.merge(i.getItem().getUid(), i.getQuantity() == null ? 0 : i.getQuantity(), Double::sum);
            names.put(i.getItem().getUid(), i.getItem().getName());
        }
        for (Map.Entry<UUID, Double> e : take.entrySet()) {
            double left = have.getOrDefault(e.getKey(), 0.0) - e.getValue();
            if (left < -1e-9) {
                return "Can't cancel: " + names.get(e.getKey()) + " from this delivery has already been sold or used (on hand "
                        + fmt(have.getOrDefault(e.getKey(), 0.0)) + ", delivery added " + fmt(e.getValue())
                        + "). Adjust stock instead.";
            }
        }
        for (StockFeedItem i : lines) i.setDeleted(true);
        f.setDeleted(true);
        f.setCancelledAt(System.currentTimeMillis());
        f.setCancelReason(reason);
        f.setCancelledBy(by);
        return null;
    }

    // ------------------------------------------------------------------ stock adjustments

    public static String cancelMovement(UUID id, String reason, String by) {
        return tx(em -> {
            StockMovement m = em.find(StockMovement.class, id);
            if (m == null) return "Entry not found.";
            if (m.isDeleted()) return "This entry is already cancelled.";
            double d = m.getQuantityDelta() == null ? 0 : m.getQuantityDelta();
            if (d > 0 && m.getItem() != null) {
                double have = StockService.onHand(em, m.getItem().getUid());
                if (have - d < -1e-9) return "Can't cancel: that stock has already been sold (on hand " + fmt(have) + ").";
            }
            m.setDeleted(true);
            m.setNotes((m.getNotes() == null ? "" : m.getNotes() + " ") + "[cancelled by " + by + ": " + reason + "]");
            return null;
        });
    }

    // ------------------------------------------------------------------ supplier + customer payments

    public static String cancelSupplierPayment(UUID id, String reason, String by) {
        return tx(em -> {
            SupplierPayment p = em.find(SupplierPayment.class, id);
            if (p == null) return "Payment not found.";
            if (p.isDeleted()) return "This payment is already cancelled.";
            p.setDeleted(true);
            p.setCancelledAt(System.currentTimeMillis());
            p.setCancelReason(reason);
            p.setCancelledBy(by);
            return null;
        });
    }

    /** Cancels a customer's debt payment: the amount is owed again and a PAYMENT_VOID entry records it. */
    public static String cancelCustomerPayment(UUID txId, String reason, String by) {
        return tx(em -> {
            CreditTransaction t = em.find(CreditTransaction.class, txId);
            if (t == null || !"PAYMENT".equals(t.getType())) return "Payment not found.";
            if (Boolean.TRUE.equals(t.getCancelled())) return "This payment is already cancelled.";
            Customer c = em.find(Customer.class, t.getCustomer().getUid());
            double amt = t.getAmount() == null ? 0 : t.getAmount();
            double after = (c.getCurrentBalance() == null ? 0 : c.getCurrentBalance()) + amt;
            c.setCurrentBalance(after);
            t.setCancelled(true);
            t.setCancelReason(reason);
            em.persist(CreditTransaction.builder().customer(c).type("PAYMENT_VOID").amount(amt).balanceAfter(after)
                    .createdAt(System.currentTimeMillis()).notes("Payment cancelled by " + by + ": " + reason).build());
            return null;
        });
    }

    // ------------------------------------------------------------------ helpers

    private static String fmt(double d) { return d == Math.floor(d) ? String.format("%.0f", d) : String.format("%.2f", d); }
    private static Long lng(Object o) { return o == null ? null : ((Number) o).longValue(); }
    private static Double dbl(Object o) { return o == null ? null : ((Number) o).doubleValue(); }
}
