package org.PSmart.POS.Repositories;

import jakarta.persistence.EntityManager;
import org.PSmart.POS.Entities.Expense;

import java.util.List;
import java.util.function.Supplier;

public class ExpenseRepository extends BaseRepository<Expense> {

    public ExpenseRepository(Supplier<EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<Expense> findAllExpenses() {
        try {
            EntityManager em = getEntityManager();
            var query = em.createQuery(
                    "FROM Expense e WHERE e.deleted=false ORDER BY e.createdAt DESC", Expense.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding all expenses", e);
        }
    }

    public List<Expense> findExpensesByDateRange(Long from, Long to) {
        try {
            EntityManager em = getEntityManager();
            var query = em.createQuery(
                    "FROM Expense e WHERE e.deleted=false AND e.createdAt BETWEEN :from AND :to ORDER BY e.createdAt DESC",
                    Expense.class);
            query.setParameter("from", from);
            query.setParameter("to", to);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding expenses by date range", e);
        }
    }

    public List<Expense> findExpensesByCategory(String category) {
        try {
            EntityManager em = getEntityManager();
            var query = em.createQuery(
                    "FROM Expense e WHERE e.deleted=false AND e.category = :cat ORDER BY e.createdAt DESC",
                    Expense.class);
            query.setParameter("cat", category);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding expenses by category", e);
        }
    }
}
