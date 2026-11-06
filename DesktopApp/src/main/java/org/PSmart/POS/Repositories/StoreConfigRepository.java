package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.StoreConfig;

import java.util.function.Supplier;

public class StoreConfigRepository extends BaseRepository<StoreConfig> {

    public StoreConfigRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public StoreConfig getStoreConfig() {
        try {
            var list = findAll();
            return list.isEmpty() ? null : list.get(0);
        } catch (Exception e) {
            return null;
        }
    }

    public StoreConfig saveOrUpdate(StoreConfig config) {
        StoreConfig existing = getStoreConfig();
        if (existing != null) {
            config.setId(existing.getId());
            return update(config);
        } else {
            return save(config);
        }
    }
}
