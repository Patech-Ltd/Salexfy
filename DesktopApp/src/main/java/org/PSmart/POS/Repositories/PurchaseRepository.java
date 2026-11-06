package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.Purchase;

import java.util.List;
import java.util.function.Supplier;

public class PurchaseRepository extends BaseRepository<Purchase> {

    public PurchaseRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<Purchase> findAllPurchases() {
        try {
            EntityManager em = getEntityManager();
            var query = em.createQuery(
                    "FROM Purchase p WHERE p.deleted=false ORDER BY p.createdAt DESC", Purchase.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all purchases", e);
        }
    }

    public List<Purchase> findPurchasesByDateRange(Long from, Long to) {
        try {
            EntityManager em = getEntityManager();
            var query = em.createQuery(
                    "FROM Purchase p WHERE p.deleted=false AND p.createdAt BETWEEN :from AND :to ORDER BY p.createdAt DESC",
                    Purchase.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding purchases by date range", e);
        }
    }
}
