//package org.PSmart.POS.Repositories;
//
//import org.PSmart.POS.Entities.WholeScale;
//import org.PSmart.POS.Main;
//import org.hibernate.Session;
//
//import java.util.List;
//
//import org.hibernate.Transaction;
//import org.hibernate.query.Query;
//
//public class WholeScaleRepository{
//
//    public static Session dbsession;
//    static {
//        dbsession= Main.getDbSession();
//    }
//
//    public WholeScale findByName(String name)
//    {
//        String hql = "FROM WholeScale c WHERE c.name = :name";
//        WholeScale variable = Main.getDbSession().createQuery(hql, WholeScale.class)
//                .setParameter("name", name)
//                .uniqueResult();
//        return variable;
//    }
//
//    public static List<WholeScale> findAll() {
//        Query<WholeScale> query = Main.getDbSession().createQuery("FROM WholeScale", WholeScale.class);
//        return query.list();
//    }
//
//    public WholeScale findById(Long id)
//    {
//        String hql = "FROM WholeScale c WHERE c.num = :num";
//        WholeScale variable = Main.getDbSession().createQuery(hql, WholeScale.class)
//                .setParameter("num", id)
//                .uniqueResult();
//        return variable;
//    }
//
//    public static void save(WholeScale variable)
//    {
//        Transaction transaction=Main.getDbSession().beginTransaction();
//        Main.getDbSession().save(variable);
//        transaction.commit();
//    }
//
//    public void update(WholeScale variable)
//    {
//        Transaction transaction=Main.getDbSession().beginTransaction();
//        Main.getDbSession().update(variable);
//        transaction.commit();
//    }
//
//    public List<WholeScale> findAllName(String name)
//    {
//        String hql = "FROM WholeScale c WHERE c.name like '%:name%' ";
//        return Main.getDbSession().createQuery(hql, WholeScale.class)
//                .setParameter("name", name)
//                .list();
//    }
//
//
//
//
//}
