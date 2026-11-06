package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.Sale;
import org.PSmart.POS.Entities.SaleItem;
import org.PSmart.POS.Entities.StoreConfig;
import org.PSmart.POS.Database.DatabaseUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class SaleRepository extends BaseRepository<Sale> {

    public SaleRepository(java.util.function.Supplier<jakarta.persistence.EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<Sale> findAllSales() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false ORDER BY s.createdAt DESC", Sale.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all sales", e);
        }
    }

    public List<Sale> findSalesByDateRange(Long from, Long to) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false AND s.createdAt BETWEEN :from AND :to ORDER BY s.createdAt DESC",
                    Sale.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding sales by date range", e);
        }
    }

    public List<Sale> findSalesByCashier(UUID cashierId) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false AND s.cashier.uid = :cashierId ORDER BY s.createdAt DESC",
                    Sale.class);
            query.setParameter("cashierId", cashierId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding sales by cashier", e);
        }
    }

    public Double getTotalSalesToday() {
        try {
            EntityManager em = getEntityManager();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            Long startOfDay = cal.getTimeInMillis();

            TypedQuery<Double> query = em.createQuery(
                    "SELECT COALESCE(SUM(s.total), 0) FROM Sale s WHERE s.deleted=false AND s.createdAt >= :startOfDay",
                    Double.class);
            query.setParameter("startOfDay", startOfDay);
            return query.getSingleResult();
        } catch (Exception e) {
            return 0.0;
        }
    }

    // ------------------------------------------------------------------
    // Pagination + search (scalable, avoids loading all rows at once)
    // ------------------------------------------------------------------

    public List<Sale> findSalesPage(int page, int size) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false ORDER BY s.createdAt DESC", Sale.class);
            query.setFirstResult(Math.max(page, 0) * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding sales page", e);
        }
    }

    public long countSales() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(s) FROM Sale s WHERE s.deleted=false", Long.class);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RepositoryException("Error counting sales", e);
        }
    }

    /** Most recent non-deleted sales, newest first. */
    public List<Sale> findRecentSales(int count) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "SELECT s FROM Sale s WHERE s.deleted=false ORDER BY s.createdAt DESC", Sale.class);
            query.setMaxResults(Math.max(1, count));
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding recent sales", e);
        }
    }

    /**
     * Search sales by receipt number or customer name, paginated.
     * Receipt numbers look like "RCP-00635B"; the suffix is a hex code derived
     * from the sale's uid hashCode. We try to match the sale id from the raw
     * uid string too (allows pasting the plain UUID).
     */
    public List<Sale> findSalesByKeywordPage(String keyword, int page, int size) {
        try {
            EntityManager em = getEntityManager();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false " +
                    "AND (LOWER(CAST(s.uid AS string)) LIKE :like " +
                    "OR LOWER(CAST(s.uid AS string)) LIKE :likeDigit " +
                    "OR (s.customer IS NOT NULL AND LOWER(s.customer.fullname) LIKE :like)) " +
                    "ORDER BY s.createdAt DESC", Sale.class);
            query.setParameter("like", like);
            query.setParameter("likeDigit", "%" + keyword.toLowerCase().trim() + "%");
            query.setFirstResult(Math.max(page, 0) * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error searching sales", e);
        }
    }

    public long countSalesByKeyword(String keyword) {
        try {
            EntityManager em = getEntityManager();
            String like = "%" + keyword.toLowerCase().trim() + "%";
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(s) FROM Sale s WHERE s.deleted=false " +
                    "AND (LOWER(CAST(s.uid AS string)) LIKE :like " +
                    "OR (s.customer IS NOT NULL AND LOWER(s.customer.fullname) LIKE :like))",
                    Long.class);
            query.setParameter("like", like);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RepositoryException("Error counting sales by keyword", e);
        }
    }

    public List<Sale> findSalesByDateRangePage(Long from, Long to, int page, int size) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Sale> query = em.createQuery(
                    "FROM Sale s WHERE s.deleted=false AND s.createdAt BETWEEN :from AND :to ORDER BY s.createdAt DESC",
                    Sale.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            query.setFirstResult(Math.max(page, 0) * size);
            query.setMaxResults(size);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding sales by date range page", e);
        }
    }

    public long countSalesByDateRange(Long from, Long to) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(s) FROM Sale s WHERE s.deleted=false AND s.createdAt BETWEEN :from AND :to",
                    Long.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getSingleResult();
        } catch (Exception e) {
            throw new RepositoryException("Error counting sales by date range", e);
        }
    }

    /** Mark a sale and all its items as deleted. */
    public void deleteSale(Sale sale) {
        try {
            EntityManager em = getEntityManager();
            em.getTransaction().begin();
            Sale managed = em.merge(sale);
            managed.setDeleted(true);
            if (managed.getItems() != null) {
                for (SaleItem si : managed.getItems()) si.setDeleted(true);
            }
            em.getTransaction().commit();
        } catch (Exception e) {
            if (getEntityManager().getTransaction().isActive())
                getEntityManager().getTransaction().rollback();
            throw new RepositoryException("Error deleting sale", e);
        }
    }

    public Sale findSaleById(java.util.UUID id) {
        try {
            return getEntityManager().find(Sale.class, id);
        } catch (Exception e) {
            throw new RepositoryException("Error finding sale by id", e);
        }
    }

    /** Recompute sale totals from its current items and persist. Returns updated sale. */
    public Sale reprocessSale(Sale sale) {
        try {
            EntityManager em = getEntityManager();
            em.getTransaction().begin();
            Sale managed = em.merge(sale);

            double sub = 0;
            double disc = 0;
            if (managed.getItems() != null) {
                for (SaleItem si : managed.getItems()) {
                    if (si.isDeleted()) continue;
                    double lineTotal = si.getUnitPrice() * si.getQuantity() - (si.getDiscount() != null ? si.getDiscount() : 0);
                    if (lineTotal < 0) lineTotal = 0;
                    si.setTotal(lineTotal);
                    sub += si.getUnitPrice() * si.getQuantity();
                    disc += si.getDiscount() != null ? si.getDiscount() : 0;
                }
            }
            managed.setSubtotal(sub);
            managed.setDiscount(disc);
            StoreConfig cfg = StoreConfig.defaultConfig();
            try {
                StoreConfig live = DatabaseUtils.getInstance().getStoreConfigRepository().getStoreConfig();
                if (live != null) cfg = live;
            } catch (Exception ignored) {}
            double vat = cfg.calculateTax(sub, disc);
            managed.setVatAmount(vat);
            managed.setTotal(cfg.calculateTotal(sub, disc));
            if (managed.getTotal() < 0) managed.setTotal(0.0);
            double diff = (managed.getAmountPaid() != null ? managed.getAmountPaid() : 0) - managed.getTotal();
            managed.setChange(diff > 0 ? diff : 0.0);

            em.getTransaction().commit();
            return managed;
        } catch (Exception e) {
            if (getEntityManager().getTransaction().isActive())
                getEntityManager().getTransaction().rollback();
            throw new RepositoryException("Error reprocessing sale", e);
        }
    }

    /** Remove a specific item line from the sale and reprocess totals. */
    public void removeSaleItem(Sale sale, SaleItem item) {
        try {
            EntityManager em = getEntityManager();
            em.getTransaction().begin();
            Sale managed = em.merge(sale);
            if (managed.getItems() != null) {
                for (SaleItem si : managed.getItems()) {
                    if (si.getUid() != null && si.getUid().equals(item.getUid())) {
                        si.setDeleted(true);
                    }
                }
            }
            em.getTransaction().commit();
            reprocessSale(managed);
        } catch (Exception e) {
            throw new RepositoryException("Error removing sale item", e);
        }
    }

    public Long getSalesCountToday() {
        try {
            EntityManager em = getEntityManager();
            java.util.Calendar cal = java.util.Calendar.getInstance();
            cal.set(java.util.Calendar.HOUR_OF_DAY, 0);
            cal.set(java.util.Calendar.MINUTE, 0);
            cal.set(java.util.Calendar.SECOND, 0);
            cal.set(java.util.Calendar.MILLISECOND, 0);
            Long startOfDay = cal.getTimeInMillis();

            TypedQuery<Long> query = em.createQuery(
                    "SELECT COUNT(s) FROM Sale s WHERE s.deleted=false AND s.createdAt >= :startOfDay",
                    Long.class);
            query.setParameter("startOfDay", startOfDay);
            return query.getSingleResult();
        } catch (Exception e) {
            return 0L;
        }
    }
}
