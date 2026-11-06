package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.HeldOrder;
import org.PSmart.POS.Entities.HeldOrderItem;
import org.PSmart.POS.Entities.Sale;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Persists held (parked) orders. The most recent {@code MAX_HELD} are kept; older
 * ones are removed automatically so the limit is never exceeded.
 */
public class HeldOrderRepository extends BaseRepository<HeldOrder> {

    public static final int MAX_HELD = 100;

    public HeldOrderRepository(java.util.function.Supplier<jakarta.persistence.EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<HeldOrder> findAllHeld() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<HeldOrder> query = em.createQuery(
                    "FROM HeldOrder h WHERE h.deleted=false ORDER BY h.createdAt DESC", HeldOrder.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error listing held orders", e);
        }
    }

    public long heldCount() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(h) FROM HeldOrder h WHERE h.deleted=false", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * Saves a held order and trims to the newest {@code MAX_HELD}.
     */
    public HeldOrder saveHeld(HeldOrder order) {
        HeldOrder saved = save(order);
        trimToMax();
        return saved;
    }

    private void trimToMax() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<HeldOrder> query = em.createQuery(
                    "FROM HeldOrder h WHERE h.deleted=false ORDER BY h.createdAt DESC", HeldOrder.class);
            List<HeldOrder> all = query.getResultList();
            if (all.size() > MAX_HELD) {
                for (int i = MAX_HELD; i < all.size(); i++) {
                    HeldOrder old = all.get(i);
                    String hql = "DELETE FROM HeldOrderItem i WHERE i.order.uid = :id";
                    em.getTransaction().begin();
                    try {
                        em.createQuery("DELETE FROM HeldOrderItem WHERE order.uid = :id").setParameter("id", old.getUid()).executeUpdate();
                        em.createQuery("DELETE FROM HeldOrder WHERE uid = :id").setParameter("id", old.getUid()).executeUpdate();
                        em.getTransaction().commit();
                    } catch (Exception e) {
                        if (em.getTransaction().isActive()) em.getTransaction().rollback();
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            throw new RepositoryException("Error trimming held orders", e);
        }
    }

    /**
     * Hard-removes a held order (once recalled/completed it should no longer be parked).
     */
    public void discardHeld(UUID id) {
        try {
            EntityManager em = getEntityManager();
            em.getTransaction().begin();
            em.createQuery("DELETE FROM HeldOrderItem WHERE order.uid = :id").setParameter("id", id).executeUpdate();
            em.createQuery("DELETE FROM HeldOrder WHERE uid = :id").setParameter("id", id).executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            throw new RepositoryException("Error discarding held order", e);
        }
    }
}