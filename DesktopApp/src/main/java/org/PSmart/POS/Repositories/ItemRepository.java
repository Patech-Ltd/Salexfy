package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.Item;
import org.PSmart.POS.TestJPA;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class ItemRepository{

    public static EntityManager entityManager;

//    public static Session dbsession;
//    static {
//        dbsession= TestJPA.getEntityManager();
//    }

    public static Item findByName(String name)
    {
        String hql = "FROM Item c WHERE c.deleted=false and c.name = :name";
        try {
            Optional<Item> variable = Optional.ofNullable(TestJPA.getEntityManager().createQuery(hql, Item.class)
                    .setParameter("name", name)
                    .getSingleResult());

            if(variable.isPresent()) {
                System.out.println(" found something "+name);
                return variable.get();
            }else
                return null;
        }catch (Exception es)
        {
            es.printStackTrace();

        }
       return null;
    }

    public static List<Item> findAll() {
        try {
            TypedQuery<Item> query = TestJPA.getEntityManager().createQuery("FROM Item i where i.deleted=false", Item.class);
            return query.getResultList();
        }catch (Exception es)
        {
            System.out.println(es);
//            throw new RuntimeException(es);
            es.printStackTrace();
            return null;
        }
    }

    public static Item findById(UUID id)
    {
        String hql = "FROM Item c WHERE c.num = :num";
        Item variable = TestJPA.getEntityManager().createQuery(hql, Item.class)
                .setParameter("num", id)
                .getSingleResult();
        return variable;
    }

    public static void save(Item variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public static void update(Item variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public static List<Item> findAllName(String name)
    {
        String hql = "FROM Item c WHERE c.deleted=false and (c.name like '%:name%' or c.description like '%:name%')";
        return TestJPA.getEntityManager().createQuery(hql, Item.class)
                .setParameter("name", name)
                .getResultList();
    }

    public static void delete(Item variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
//        variable.deleteImage(variable);
        variable.setDeleted(true);
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public static Item findByBarCode(String bcode)
    {
        Long start=new Date().getTime();
        String hql = "FROM Item c WHERE c.deleted=false and c.barcode = :code";
        Item variable = null;
        try {
            variable = TestJPA.getEntityManager().createQuery(hql, Item.class)
                    .setParameter("code", bcode)
                    .getSingleResult();
        }catch (Exception es)
        {

        }
        Long end=new Date().getTime();
        System.out.println("time for fetching "+(end-start));
        return variable;
    }


    public static List<Item> findAllAsc() {
        TypedQuery<Item> query = TestJPA.getEntityManager().createQuery("FROM Item i where i.deleted=false order by name asc", Item.class);
        return query.getResultList();
    }





}
