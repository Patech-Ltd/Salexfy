package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.StoreConfig;

import java.util.function.Supplier;

public class StoreConfigRepository extends BaseRepository<StoreConfig> {

    public StoreConfigRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    /**
     * The shop's settings row. (The generic findAll() filters on a "deleted" column that StoreConfig does not
     * have, so it always failed and the app silently fell back to defaults.) The newest row wins, which
     * is also the one the previous versions saved last.
     */
    public StoreConfig getStoreConfig() {
        EntityManager em = getEntityManager();
        try {
            var list = em.createQuery("FROM StoreConfig c ORDER BY c.id DESC", StoreConfig.class).setMaxResults(1).getResultList();
            if (list.isEmpty()) return null;
            StoreConfig c = list.get(0);
            // load the element collections while the session is open
            if (c.getPaymentMethods() != null) c.getPaymentMethods().size();
            if (c.getPaymentMethodDetails() != null) c.getPaymentMethodDetails().size();
            return c;
        } catch (Exception e) {
            return null;
        } finally {
            em.close();
        }
    }

    public StoreConfig saveOrUpdate(StoreConfig config) {
        StoreConfig existing = getStoreConfig();
        EntityManager em = getEntityManager();
        try {
            em.getTransaction().begin();
            StoreConfig result;
            if (existing != null) {
                config.setId(existing.getId());
                result = em.merge(config);
            } else {
                config.setId(null);
                em.persist(config);
                result = config;
            }
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) em.getTransaction().rollback();
            throw new RepositoryException("Error saving store settings", e);
        } finally {
            em.close();
        }
    }
}
