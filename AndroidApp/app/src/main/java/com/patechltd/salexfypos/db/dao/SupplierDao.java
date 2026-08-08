package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.DebtorBalanceRow;
import com.patechltd.salexfypos.db.entity.Customer;
import com.patechltd.salexfypos.db.entity.DebtPayment;
import com.patechltd.salexfypos.db.entity.Supplier;

import java.util.List;

@Dao
public interface SupplierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSupplier(Supplier supplier);

    @Update
    int updateSupplier(Supplier supplier);

    @Delete
    int deleteSupplier(Supplier supplier);

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    LiveData<List<Supplier>> observeSuppliers();

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    List<Supplier> getSuppliers();

    @Query("SELECT * FROM suppliers WHERE id = :id")
    Supplier getSupplier(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCustomer(Customer customer);

    @Update
    int updateCustomer(Customer customer);

    @Delete
    int deleteCustomer(Customer customer);

    @Query("SELECT * FROM customers ORDER BY name ASC")
    LiveData<List<Customer>> observeCustomers();

    @Query("SELECT * FROM customers ORDER BY name ASC")
    List<Customer> getCustomers();

    @Query("SELECT * FROM customers WHERE id = :id")
    Customer getCustomer(String id);

    @Query("SELECT * FROM customers WHERE lower(name) = lower(:name) LIMIT 1")
    Customer findCustomerByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertDebtPayment(DebtPayment payment);

    @Query("SELECT * FROM debt_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    LiveData<List<DebtPayment>> observePayments(String customerId);

    @Query("SELECT * FROM debt_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    List<DebtPayment> getPayments(String customerId);

    @Query("SELECT c.id AS customerId, c.name, "
            + "COALESCE((SELECT SUM(s.total - s.paidAmount) FROM sales s WHERE s.customerId = c.id AND s.status = 'COMPLETE'), 0) "
            + "- COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.customerId = c.id), 0) AS outstanding "
            + "FROM customers c ORDER BY c.name ASC")
    LiveData<List<DebtorBalanceRow>> observeDebtorBalances();

    @Query("SELECT c.id AS customerId, c.name, "
            + "COALESCE((SELECT SUM(s.total - s.paidAmount) FROM sales s WHERE s.customerId = c.id AND s.status = 'COMPLETE'), 0) "
            + "- COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.customerId = c.id), 0) AS outstanding "
            + "FROM customers c ORDER BY c.name ASC")
    List<DebtorBalanceRow> getDebtorBalances();

    @Query("SELECT COALESCE(SUM(s.total - s.paidAmount), 0) FROM sales s "
            + "WHERE s.customerId = :customerId AND s.status = 'COMPLETE'")
    double customerDebt(String customerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM debt_payments p WHERE p.customerId = :customerId")
    double customerPaid(String customerId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments "
            + "WHERE paymentDate >= :from AND paymentDate <= :to")
    double paymentsBetween(long from, long to);
}
