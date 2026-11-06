package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.*;

import java.util.*;

/** Plain-column reads for the Purchases page (no entity graphs, short-lived sessions). */
public final class PurchaseQueries {

    private PurchaseQueries() {}

    public static final class Row {
        public UUID uid;
        public long createdAt;
        public String ref, supplier, invoice, status;
        public int items;
        public double total, paid;
    }

    private static EntityManager em() {
        return DatabaseUtils.getInstance().getEmf().createEntityManager();
    }

    public static List<Row> rows() {
        EntityManager em = em();
        try {
            List<Row> out = new ArrayList<>();
            for (Object o : em.createQuery("SELECT p.uid, p.createdAt, p.supplierName, p.invoiceNumber, p.status, p.total, p.paidAmount, "
                    + "(SELECT COUNT(i) FROM PurchaseItem i WHERE i.purchase = p AND i.deleted = false) "
                    + "FROM Purchase p WHERE p.deleted=false ORDER BY p.createdAt DESC").getResultList()) {
                Object[] r = (Object[]) o;
                Row x = new Row();
                x.uid = (UUID) r[0];
                x.createdAt = r[1] == null ? 0 : ((Number) r[1]).longValue();
                x.supplier = r[2] == null ? "" : (String) r[2];
                x.invoice = r[3] == null ? "" : (String) r[3];
                x.status = r[4] == null ? "" : (String) r[4];
                x.total = r[5] == null ? 0 : ((Number) r[5]).doubleValue();
                x.paid = r[6] == null ? x.total : ((Number) r[6]).doubleValue();
                x.items = r[7] == null ? 0 : ((Number) r[7]).intValue();
                x.ref = "PUR-" + String.format("%06d", x.uid.hashCode() & 0xFFFFFF);
                out.add(x);
            }
            return out;
        } finally {
            em.close();
        }
    }

    /** Read-only copy for preview/printing, built from columns. Do not persist it. */
    public static Purchase load(UUID id) {
        EntityManager em = em();
        try {
            List<?> head = em.createQuery("SELECT p.createdAt, p.supplierName, p.invoiceNumber, p.notes, p.subtotal, p.taxAmount, p.total, "
                    + "p.paidAmount, p.status, u.fullname, u.username FROM Purchase p LEFT JOIN p.createdBy u WHERE p.uid=:id")
                    .setParameter("id", id).getResultList();
            if (head.isEmpty()) return null;
            Object[] r = (Object[]) head.get(0);
            Purchase p = Purchase.builder().uid(id).createdAt(r[0] == null ? null : ((Number) r[0]).longValue())
                    .supplierName((String) r[1]).invoiceNumber((String) r[2]).notes((String) r[3])
                    .subtotal(d(r[4])).taxAmount(d(r[5])).total(d(r[6])).paidAmount(d(r[7])).status((String) r[8]).build();
            if (r[9] != null || r[10] != null) {
                User u = new User();
                u.setFullname((String) r[9]);
                u.setUsername((String) r[10]);
                p.setCreatedBy(u);
            }
            List<PurchaseItem> items = new ArrayList<>();
            for (Object o : em.createQuery("SELECT it.name, i.quantity, i.unitPrice, i.total FROM PurchaseItem i LEFT JOIN i.item it "
                    + "WHERE i.purchase.uid=:id AND i.deleted=false").setParameter("id", id).getResultList()) {
                Object[] x = (Object[]) o;
                items.add(PurchaseItem.builder().item(Item.builder().name(x[0] == null ? "(item)" : (String) x[0]).build())
                        .quantity(d(x[1])).unitPrice(d(x[2])).total(d(x[3])).build());
            }
            p.setItems(items);
            return p;
        } finally {
            em.close();
        }
    }

    private static Double d(Object o) {
        return o == null ? null : ((Number) o).doubleValue();
    }
}
