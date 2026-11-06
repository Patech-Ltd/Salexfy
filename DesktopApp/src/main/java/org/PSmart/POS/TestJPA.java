package org.PSmart.POS;

import org.PSmart.POS.Entities.Item;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import java.util.List;

public class TestJPA {

    private static EntityManagerFactory entityManagerFactory;
    private static EntityManager entityManager;

    public static void main(String[] args) {
        try {
            // Initialize the EntityManager
            entityManager = getEntityManager();

            // Perform database operations
            List<Item> itemList = retrieveAllItems();
            displayItems(itemList);

            // Close the EntityManager when done
            closeEntityManager();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static EntityManager getEntityManager() {
        if (entityManager == null || !entityManager.isOpen()) {
            if (entityManagerFactory == null || !entityManagerFactory.isOpen()) {
                entityManagerFactory = Persistence.createEntityManagerFactory("PSmartPosPU");
            }
            entityManager = entityManagerFactory.createEntityManager();
        }
        return entityManager;
    }

    private static void closeEntityManager() {
        if (entityManager != null && entityManager.isOpen()) {
            entityManager.close();
        }
        if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
            entityManagerFactory.close();
        }
    }

    private static List<Item> retrieveAllItems() {
        // Use the EntityManager to perform queries or other database operations
        return entityManager.createQuery("SELECT i FROM Item i", Item.class).getResultList();
    }

    private static void displayItems(List<Item> itemList) {
        // Display the retrieved items
        for (Item item : itemList) {
            System.out.println("Item ID: " + item.getUid() + ", Name: " + item.getName());
        }
    }
}
