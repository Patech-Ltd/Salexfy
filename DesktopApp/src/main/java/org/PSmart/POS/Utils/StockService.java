package org.PSmart.POS.Utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.Entities.StockMovement;
import org.PSmart.POS.Entities.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Single source of truth for on-hand stock. */
public final class StockService {

    private static final List<String> VOID_STATUSES = List.of("VOID", "VOIDED", "CANCELLED", "CANCELED");

    private StockService() {}

    /** On-hand per item uid = received - sold + manual movements. Items never received map to 0 or negative. */
    public static Map<UUID, Double> onHandAll(EntityManager em) {
        Map<UUID, Double> map = new HashMap<>();
        for (Object o : em.createQuery(
                "SELECT f.item.uid, COALESCE(SUM(f.quantity),0) FROM StockFeedItem f "
                        + "WHERE f.deleted=false GROUP BY f.item.uid").getResultList()) {
            Object[] r = (Object[]) o;
            map.merge((UUID) r[0], ((Number) r[1]).doubleValue(), Double::sum);
        }
        for (Object o : em.createQuery(
                "SELECT si.item.uid, COALESCE(SUM(si.quantity),0) FROM SaleItem si "
                        + "WHERE si.deleted=false AND si.sale.deleted=false "
                        + "AND (si.sale.status IS NULL OR UPPER(si.sale.status) NOT IN :bad) GROUP BY si.item.uid")
                .setParameter("bad", VOID_STATUSES).getResultList()) {
            Object[] r = (Object[]) o;
            map.merge((UUID) r[0], -((Number) r[1]).doubleValue(), Double::sum);
        }
        for (Object o : em.createQuery(
                "SELECT m.item.uid, COALESCE(SUM(m.quantityDelta),0) FROM StockMovement m "
                        + "WHERE m.deleted=false GROUP BY m.item.uid").getResultList()) {
            Object[] r = (Object[]) o;
            map.merge((UUID) r[0], ((Number) r[1]).doubleValue(), Double::sum);
        }
        return map;
    }

    public static double onHand(EntityManager em, UUID itemId) {
        Double v = onHandAll(em).get(itemId);
        return v == null ? 0 : v;
    }

    /**
     * Records a signed change atomically. Re-reads on-hand inside the transaction so the
     * before/after figures are accurate.
     */
    public static StockMovement record(UUID itemId, String type, double delta, String reason,
                                       String notes, String reference, User user) {
        EntityManager em = DatabaseUtils.getInstance().getEmf().createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            Item item = em.find(Item.class, itemId);
            if (item == null) throw new IllegalArgumentException("Item not found");
            double before = onHand(em, itemId);
            StockMovement m = StockMovement.builder()
                    .item(item).type(type).quantityDelta(delta)
                    .quantityBefore(before).quantityAfter(before + delta)
                    .reason(reason).notes(notes).reference(reference)
                    .createdByName(user == null ? null : (user.getFullname() != null ? user.getFullname() : user.getUsername()))
                    .createdAt(System.currentTimeMillis()).build();
            em.persist(m);
            tx.commit();
            return m;
        } catch (RuntimeException e) {
            if (tx.isActive()) tx.rollback();
            throw e;
        } finally {
            em.close();
        }
    }
}
