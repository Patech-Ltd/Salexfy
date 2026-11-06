package org.PSmart.POS.Repositories;

import org.PSmart.POS.Entities.CreditTransaction;
import org.PSmart.POS.Entities.Customer;
import org.PSmart.POS.Entities.Sale;
import org.PSmart.POS.Entities.User;

import java.util.List;
import java.util.UUID;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

/**
 * Repository over the customer credit ledger. A credit sale posts a CHARGE and
 * a payment against it posts a PAYMENT. The DebitRepository keeps Customer balances
 * in sync with the ledger.
 */
public class CreditRepository extends BaseRepository<CreditTransaction> {

    public CreditRepository(java.util.function.Supplier<jakarta.persistence.EntityManager> entityManagerSupplier) {
        super(entityManagerSupplier);
    }

    public List<CreditTransaction> findByCustomer(UUID customerId) {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<CreditTransaction> query = em.createQuery(
                    "FROM CreditTransaction t WHERE t.deleted=false AND t.customer.uid = :id ORDER BY t.createdAt ASC",
                    CreditTransaction.class);
            query.setParameter("id", customerId);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding credit transactions for customer", e);
        }
    }

    public List<CreditTransaction> findAllLedger() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<CreditTransaction> query = em.createQuery(
                    "FROM CreditTransaction t WHERE t.deleted=false ORDER BY t.createdAt DESC", CreditTransaction.class);
            return query.getResultList();
        } catch (Exception e) {
            throw new RepositoryException("Error finding credit ledger", e);
        }
    }

    /**
     * Posts a CHARGE for an on-credit sale and updates the customer's running balance.
     */
    public CreditTransaction recordCreditSale(Sale sale, Customer customer, User cashier) {
        Double balance = customer.getCurrentBalance() == null ? 0.0 : customer.getCurrentBalance();
        // Only the unpaid part of a split sale goes on the account (cash/M-Pesa/card already paid).
        double charge = Math.max(0, (sale.getTotal() == null ? 0 : sale.getTotal())
                - (sale.getAmountPaid() == null ? 0 : sale.getAmountPaid()));
        balance = balance + charge;

        CreditTransaction tx = CreditTransaction.builder()
                .customer(customer)
                .sale(sale)
                .type("CHARGE")
                .amount(charge)
                .balanceAfter(balance)
                .cashier(cashier)
                .createdAt(System.currentTimeMillis())
                .notes("On-credit sale " + sale.getReceiptNumber() + " (" + sale.getPaymentMethod() + ")")
                .build();

        customer.setCurrentBalance(balance);
        CustomerRepository crepo = new CustomerRepository(entityManagerSupplier);
        crepo.update(customer);
        return save(tx);
    }

    /**
     * Records a PAYMENT against a customer's debt and updates the running balance.
     * @return the created ledger entry (with the new balance) or null if amount invalid.
     */
    public CreditTransaction recordPayment(Customer customer, double amount, User cashier, String notes) {
        if (amount <= 0) return null;

        EntityManager em = getEntityManager();
        Customer managed = em.find(Customer.class, customer.getUid());
        Double balance = managed.getCurrentBalance() == null ? 0.0 : managed.getCurrentBalance();
        double newBalance = Math.max(0.0, balance - amount);

        CreditTransaction tx = CreditTransaction.builder()
                .customer(managed)
                .type("PAYMENT")
                .amount(amount)
                .balanceAfter(newBalance)
                .cashier(cashier)
                .createdAt(System.currentTimeMillis())
                .notes(notes == null ? "Debt payment" : notes)
                .build();
        save(tx);

        managed.setCurrentBalance(newBalance);
        CustomerRepository crepo = new CustomerRepository(entityManagerSupplier);
        crepo.update(managed);
        return tx;
    }

    /** Sum of all outstanding balances across un-deleted customers. */
    public Double getTotalOutstanding() {
        try {
            EntityManager em = getEntityManager();
            TypedQuery<Double> query = em.createQuery(
                    "SELECT COALESCE(SUM(c.currentBalance), 0) FROM Customer c WHERE c.deleted=false AND c.currentBalance > 0",
                    Double.class);
            return query.getSingleResult();
        } catch (Exception e) {
            return 0.0;
        }
    }
}