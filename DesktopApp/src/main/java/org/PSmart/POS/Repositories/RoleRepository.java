package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.Role;

import java.util.function.Supplier;

public class RoleRepository extends BaseRepository<Role>{

    public RoleRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    protected RoleRepository(Class<Role> entityClass, Supplier<EntityManager> entityManagerSupplier) {
        super(entityClass, entityManagerSupplier);
    }
}
