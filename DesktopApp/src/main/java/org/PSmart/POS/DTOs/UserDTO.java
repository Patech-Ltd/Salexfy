package org.PSmart.POS.DTOs;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.User;

import java.util.Collections;
import java.util.List;

public class UserDTO {

    public static List<User> listUsers()
    {
        EntityManager entityManager = DatabaseUtils.getInstance().getEmf().createEntityManager();

        return Collections.emptyList();
    }



}
