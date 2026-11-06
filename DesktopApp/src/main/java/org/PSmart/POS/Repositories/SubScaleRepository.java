package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.SubScale;
import org.PSmart.POS.Main;
import org.PSmart.POS.TestJPA;
import org.hibernate.Session;

import java.util.List;

import org.hibernate.Transaction;
import org.hibernate.query.Query;

import jakarta.persistence.EntityTransaction;
import jakarta.persistence.TypedQuery;

public class SubScaleRepository{

//    public static Session dbsession;
//    static {
//        dbsession= TestJPA.getEntityManager();
//    }

    public SubScale findByName(String name)
    {
        String hql = "FROM SubScale c WHERE c.name = :name";
        SubScale variable = TestJPA.getEntityManager().createQuery(hql, SubScale.class)
                .setParameter("name", name)
                .getSingleResult();
        return variable;
    }

    public static List<SubScale> findAll() {
        TypedQuery<SubScale> query = TestJPA.getEntityManager().createQuery("FROM SubScale", SubScale.class);
        return query.getResultList();
    }

    public SubScale findById(Long id)
    {
        String hql = "FROM SubScale c WHERE c.num = :num";
        SubScale variable = TestJPA.getEntityManager().createQuery(hql, SubScale.class)
                .setParameter("num", id)
                .getSingleResult();
        return variable;
    }

    public static void save(SubScale variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public void update(SubScale variable)
    {
        EntityTransaction transaction=TestJPA.getEntityManager().getTransaction();
        transaction.begin();
        TestJPA.getEntityManager().persist(variable);
        transaction.commit();
    }

    public List<SubScale> findAllName(String name)
    {
        String hql = "FROM SubScale c WHERE c.name like '%:name%' ";
        return TestJPA.getEntityManager().createQuery(hql, SubScale.class)
                .setParameter("name", name)
                .getResultList();
    }




}
