package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.WholeScale;
import org.PSmart.POS.Main;
import org.PSmart.POS.TestJPA;
import org.hibernate.Session;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.query.Query;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public  class WholeScalesRepository {

//    public static  Session dbsession;
//    static {
//        dbsession= TestJPA.getEntityManager();
//    }

    public static  WholeScale findByName(String name)
    {
        String hql = "FROM WholeScale c WHERE c.name = :name";
        WholeScale variable = TestJPA.getEntityManager().createQuery(hql, WholeScale.class)
                .setParameter("name", name)
                .getSingleResult();
        return variable;
    }

    public static  List<WholeScale> findAll() {
        TypedQuery<WholeScale> query = TestJPA.getEntityManager().createQuery("FROM WholeScale", WholeScale.class);
        return query.getResultList();
    }

    public static  WholeScale findById(Long id)
    {
        String hql = "FROM WholeScale c WHERE c.num = :num";
        WholeScale variable = TestJPA.getEntityManager().createQuery(hql, WholeScale.class)
                .setParameter("num", id)
                .getSingleResult();
        return variable;
    }

    public static void save(WholeScale variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public static  void update(WholeScale variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public static  List<WholeScale> findAllName(String name)
    {
        String hql = "FROM WholeScale c WHERE c.name like '%:name%' ";
        return TestJPA.getEntityManager().createQuery(hql, WholeScale.class)
                .setParameter("name", name)
                .getResultList();
    }




}
