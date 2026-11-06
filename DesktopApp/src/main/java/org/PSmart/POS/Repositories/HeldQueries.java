package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Customer;

import java.util.*;

/** Plain-column reads for held (parked) sales, so the Cashier never loads entity graphs to list them. */
public final class HeldQueries {

    private HeldQueries() {}

    public static final class Held {
        public UUID uid;
        public long createdAt;
        public UUID customerId;
        public String customerName, note;
        public double total;
        public int lines;
        public String summary = "";
    }

    public static final class Line {
        public UUID itemId;
        public String name;
        public double quantity;
    }

    private static EntityManager em() {
        return DatabaseUtils.getInstance().getEmf().createEntityManager();
    }

    public static List<Held> list() {
        EntityManager em = em();
        try {
            List<Held> out = new ArrayList<>();
            Map<UUID, Held> byId = new HashMap<>();
            for (Object o : em.createQuery("SELECT h.uid, h.createdAt, c.uid, c.fullname, h.note, h.total FROM HeldOrder h "
                    + "LEFT JOIN h.customer c WHERE h.deleted=false ORDER BY h.createdAt DESC").getResultList()) {
                Object[] r = (Object[]) o;
                Held h = new Held();
                h.uid = (UUID) r[0];
                h.createdAt = r[1] == null ? 0 : ((Number) r[1]).longValue();
                h.customerId = (UUID) r[2];
                h.customerName = r[3] == null ? "Walk-in" : (String) r[3];
                h.note = r[4] == null ? "" : (String) r[4];
                h.total = r[5] == null ? 0 : ((Number) r[5]).doubleValue();
                out.add(h);
                byId.put(h.uid, h);
            }
            if (!out.isEmpty()) {
                for (Object o : em.createQuery("SELECT i.order.uid, it.name, i.quantity FROM HeldOrderItem i LEFT JOIN i.item it "
                        + "WHERE i.deleted=false AND i.order.deleted=false ORDER BY i.uid").getResultList()) {
                    Object[] r = (Object[]) o;
                    Held h = byId.get((UUID) r[0]);
                    if (h == null) continue;
                    h.lines++;
                    if (h.lines <= 2) {
                        double q = r[2] == null ? 0 : ((Number) r[2]).doubleValue();
                        h.summary += (h.summary.isEmpty() ? "" : ", ") + String.format("%.0f x %s", q, r[1] == null ? "?" : r[1]);
                    }
                }
                for (Held h : out) if (h.lines > 2) h.summary += ", +" + (h.lines - 2) + " more";
            }
            return out;
        } finally {
            em.close();
        }
    }

    public static List<Line> lines(UUID heldId) {
        EntityManager em = em();
        try {
            List<Line> out = new ArrayList<>();
            for (Object o : em.createQuery("SELECT it.uid, it.name, i.quantity FROM HeldOrderItem i JOIN i.item it "
                    + "WHERE i.order.uid=:id AND i.deleted=false").setParameter("id", heldId).getResultList()) {
                Object[] r = (Object[]) o;
                Line l = new Line();
                l.itemId = (UUID) r[0];
                l.name = (String) r[1];
                l.quantity = r[2] == null ? 1 : ((Number) r[2]).doubleValue();
                out.add(l);
            }
            return out;
        } finally {
            em.close();
        }
    }

    /** A product loaded in its own short session (scale options are read before the session closes). */
    public static org.PSmart.POS.Entities.Item item(UUID id) {
        EntityManager em = em();
        try {
            org.PSmart.POS.Entities.Item it = em.find(org.PSmart.POS.Entities.Item.class, id);
            if (it != null) {
                it.getWholeScales();
                it.getSubScales();
                // detach copies of the scale lists so the cart can use them after the session closes
                it.setWholeScales(it.getWholeScales());
                it.setSubScales(it.getSubScales());
            }
            return it;
        } finally {
            em.close();
        }
    }

    public static Customer customer(UUID id) {
        if (id == null) return null;
        EntityManager em = em();
        try {
            return em.find(Customer.class, id);
        } finally {
            em.close();
        }
    }
}
