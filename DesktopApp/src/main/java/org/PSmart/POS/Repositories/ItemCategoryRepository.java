package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.ItemCategory;
import org.PSmart.POS.TestJPA;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class ItemCategoryRepository{

//    public static Session dbsession;
//    static {
//        dbsession= TestJPA.getEntityManager();
//    }

    public ItemCategory findByName(String name)
    {
        String hql = "FROM ItemCategory c WHERE c.name = :name";
        ItemCategory itemCategory = TestJPA.getEntityManager().createQuery(hql, ItemCategory.class)
                .setParameter("name", name)
                .getSingleResult();
        return itemCategory;
    }

    public static List<ItemCategory> findAll() {
            TypedQuery<ItemCategory> query = TestJPA.getEntityManager().createQuery("FROM ItemCategory", ItemCategory.class);
            return query.getResultList();
    }

    public static ItemCategory findById(UUID id)
    {
        String hql = "FROM ItemCategory c WHERE c.num = :num";
        ItemCategory itemCategory = TestJPA.getEntityManager().createQuery(hql, ItemCategory.class)
                .setParameter("num", id)
                .getSingleResult();
        return itemCategory;
    }

    public static void save(ItemCategory itemCategory)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(itemCategory);
        transaction.commit();
    }

    public static void update(ItemCategory itemCategory)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(itemCategory);
        transaction.commit();
    }

    public List<ItemCategory> findAllName(String name)
    {
        String hql = "FROM ItemCategory c WHERE c.name like '%:name%' or c.description like '%:name%'";
        return TestJPA.getEntityManager().createQuery(hql, ItemCategory.class)
                .setParameter("name", name)
                .getResultList();
    }

    public static List<ItemCategory> findAllAsc() {
        TypedQuery<ItemCategory> query = TestJPA.getEntityManager().createQuery("FROM ItemCategory order by name asc", ItemCategory.class);
        return query.getResultList();
    }




}
