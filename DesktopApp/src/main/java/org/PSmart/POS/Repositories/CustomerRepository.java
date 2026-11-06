package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.Customer;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

public class CustomerRepository extends BaseRepository<Customer> {

    public CustomerRepository(java.util.function.Supplier<jakarta.persistence.EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<Customer> findAllCustomers() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Customer> query = em.createQuery(
                    "FROM Customer c WHERE c.deleted=false ORDER BY c.fullname ASC", Customer.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all customers", e);
        }
    }

    public Customer findByPhone(String phone) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Customer> query = em.createQuery(
                    "FROM Customer c WHERE c.deleted=false AND c.phone = :phone", Customer.class);
            query.setParameter("phone", phone);
            return query.getSingleResult();
        } catch (Exception e) {
            return null;
        }
    }

    public List<Customer> searchCustomers(String searchTerm) {
        try {
            EntityManager em = getEntityManager();
            String likePattern = "%" + searchTerm.toLowerCase() + "%";
            TypedQuery<Customer> query = em.createQuery(
                    "FROM Customer c WHERE c.deleted=false AND (LOWER(c.fullname) LIKE :term OR LOWER(c.phone) LIKE :term OR LOWER(c.email) LIKE :term)",
                    Customer.class);
            query.setParameter("term", likePattern);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error searching customers", e);
        }
    }
}
