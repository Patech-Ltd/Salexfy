package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.User;

import java.util.function.Supplier;

public class UsersRepository extends BaseRepository<User>{
    public UsersRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    protected UsersRepository(Class<User> entityClass, Supplier<EntityManager> entityManagerSupplier) {
        super(entityClass, entityManagerSupplier);
    }
}
