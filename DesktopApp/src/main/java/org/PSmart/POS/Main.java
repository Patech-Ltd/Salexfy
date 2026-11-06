package org.PSmart.POS;


import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import org.PSmart.POS.Database.DatabaseUtils;
import org.PSmart.POS.Entities.Role;
import org.PSmart.POS.Entities.RoleType;
import org.PSmart.POS.Entities.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class Main {




    public static void main(String[] args) {
        try {

            EntityManager entityManager = DatabaseUtils.getInstance().getEmf().createEntityManager();

            User existing = null;
            try {
                existing = entityManager.createQuery(
                        "FROM User u WHERE u.username = :username", User.class)
                        .setParameter("username", "pats")
                        .getSingleResult();
            } catch (Exception ign) {
                // not found
            }

            if (existing == null) {
                EntityTransaction transaction = entityManager.getTransaction();
                transaction.begin();

                User sr = new User();
                sr.setFullname("Patrick Githinji Ngunjiri");
                sr.setPhone("0723466505");
                sr.setPassword("123");
                sr.setUsername("pats");

                try {
                    entityManager.persist(sr);
                    entityManager.flush();

                    for (RoleType type : RoleType.values()) {
                        Role role = new Role();
                        role.setRoleType(type);
                        role.setView(true);
                        role.setChange(true);
                        role.setUser(sr);
                        entityManager.persist(role);
                    }

                    transaction.commit();
                    System.out.println(sr);
                } catch (Exception edytt) {
                    System.out.println(edytt);
                    try { if (transaction.isActive()) transaction.rollback(); } catch (Exception ign) {}
                }
            } else {
                List<RoleType> hasRoles = new ArrayList<>();
                if (existing.getRoles() != null)
                    for (Role r : existing.getRoles()) hasRoles.add(r.getRoleType());

                List<RoleType> missing = new ArrayList<>();
                for (RoleType type : RoleType.values())
                    if (!hasRoles.contains(type)) missing.add(type);

                if (!missing.isEmpty()) {
                    EntityTransaction transaction = entityManager.getTransaction();
                    transaction.begin();
                    try {
                        existing = entityManager.find(User.class, existing.getUid());
                        for (RoleType type : missing) {
                            Role role = new Role();
                            role.setRoleType(type);
                            role.setView(true);
                            role.setChange(true);
                            role.setUser(existing);
                            entityManager.persist(role);
                        }
                        transaction.commit();
                    } catch (Exception edytt) {
                        System.out.println(edytt);
                        try { if (transaction.isActive()) transaction.rollback(); } catch (Exception ign) {}
                    }
                }
            }

            org.PSmart.POS.Utils.AutoBackupScheduler.start();
            new POSApp(null);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);

            e.printStackTrace();
        }

    }






}