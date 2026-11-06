package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.CartLine;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Persists the cashier's working cart. One active cart line set is kept: every
 * edit rewrites the single stored cart, so an in-progress sale survives restarts.
 */
public class CartRepository extends BaseRepository<CartLine> {

    public CartRepository(java.util.function.Supplier<jakarta.persistence.EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<CartLine> findActiveCart() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<CartLine> query = em.createQuery(
                    "FROM CartLine c WHERE c.deleted=false ORDER BY c.updatedAt ASC", CartLine.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error loading active cart", e);
        }
    }

    /** Replaces the entire stored cart with the given lines (persistent overwrite). */
    public void replaceCart(List<CartLine> lines) {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            em.createQuery("DELETE FROM CartLine").executeUpdate();
            for (CartLine line : lines) {
                if (line.getItem() == null) continue;
                line.setUpdatedAt(System.currentTimeMillis());
                line.setDeleted(false);
                em.persist(line);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RepositoryException("Error replacing cart", e);
        }
    }

    public void clearCart() {
        EntityManager em = getEntityManager();
        em.getTransaction().begin();
        try {
            em.createQuery("DELETE FROM CartLine").executeUpdate();
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RepositoryException("Error clearing cart", e);
        }
    }
}