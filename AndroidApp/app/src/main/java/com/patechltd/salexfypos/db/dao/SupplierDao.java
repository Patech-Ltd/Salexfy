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
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class SupplierDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSupplierRaw(Supplier supplier);

    @Update
    abstract int updateSupplierRaw(Supplier supplier);

    @Delete
    abstract int deleteSupplierRaw(Supplier supplier);

    public long insertSupplier(Supplier supplier) {
        long id = insertSupplierRaw(supplier);
        SyncTracker.track(SyncTracker.SUPPLIER, supplier.uid, "INSERT", SyncSerializer.toJson(supplier));
        return id;
    }

    public int updateSupplier(Supplier supplier) {
        int rows = updateSupplierRaw(supplier);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.SUPPLIER, supplier.uid, "UPDATE", SyncSerializer.toJson(supplier));
        }
        return rows;
    }

    public int deleteSupplier(Supplier supplier) {
        int rows = deleteSupplierRaw(supplier);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.SUPPLIER, supplier.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    public abstract LiveData<List<Supplier>> observeSuppliers();

    @Query("SELECT * FROM suppliers ORDER BY name ASC")
    public abstract List<Supplier> getSuppliers();

    @Query("SELECT * FROM suppliers WHERE id = :id")
    public abstract Supplier getSupplier(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertCustomerRaw(Customer customer);

    @Update
    abstract int updateCustomerRaw(Customer customer);

    @Delete
    abstract int deleteCustomerRaw(Customer customer);

    public long insertCustomer(Customer customer) {
        long id = insertCustomerRaw(customer);
        SyncTracker.track(SyncTracker.CUSTOMER, customer.uid, "INSERT", SyncSerializer.toJson(customer));
        return id;
    }

    public int updateCustomer(Customer customer) {
        int rows = updateCustomerRaw(customer);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.CUSTOMER, customer.uid, "UPDATE", SyncSerializer.toJson(customer));
        }
        return rows;
    }

    public int deleteCustomer(Customer customer) {
        int rows = deleteCustomerRaw(customer);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.CUSTOMER, customer.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT * FROM customers ORDER BY name ASC")
    public abstract LiveData<List<Customer>> observeCustomers();

    @Query("SELECT * FROM customers ORDER BY name ASC")
    public abstract List<Customer> getCustomers();

    @Query("SELECT * FROM customers WHERE id = :id")
    public abstract Customer getCustomer(String id);

    @Query("SELECT * FROM customers WHERE lower(name) = lower(:name) LIMIT 1")
    public abstract Customer findCustomerByName(String name);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertDebtPaymentRaw(DebtPayment payment);

    public long insertDebtPayment(DebtPayment payment) {
        long id = insertDebtPaymentRaw(payment);
        SyncTracker.track(SyncTracker.DEBT_PAYMENT, payment.uid, "INSERT", SyncSerializer.toJson(payment));
        return id;
    }

    @Query("DELETE FROM debt_payments WHERE id = :id")
    public abstract void rawDeleteDebtPayment(String id);

    @Query("SELECT * FROM debt_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    public abstract LiveData<List<DebtPayment>> observePayments(String customerId);

    @Query("SELECT * FROM debt_payments WHERE customerId = :customerId ORDER BY paymentDate DESC")
    public abstract List<DebtPayment> getPayments(String customerId);

    @Query("SELECT c.id AS customerId, c.name, c.phone, "
            + "COALESCE((SELECT SUM(s.total - s.paidAmount) FROM sales s WHERE s.customerId = c.id AND s.status = 'COMPLETE'), 0) "
            + "- COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.customerId = c.id), 0) AS outstanding "
            + "FROM customers c ORDER BY c.name ASC")
    public abstract LiveData<List<DebtorBalanceRow>> observeDebtorBalances();

    @Query("SELECT c.id AS customerId, c.name, c.phone, "
            + "COALESCE((SELECT SUM(s.total - s.paidAmount) FROM sales s WHERE s.customerId = c.id AND s.status = 'COMPLETE'), 0) "
            + "- COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.customerId = c.id), 0) AS outstanding "
            + "FROM customers c ORDER BY c.name ASC")
    public abstract List<DebtorBalanceRow> getDebtorBalances();

    @Query("SELECT COALESCE(SUM(s.total - s.paidAmount), 0) FROM sales s "
            + "WHERE s.customerId = :customerId AND s.status = 'COMPLETE'")
    public abstract double customerDebt(String customerId);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM debt_payments p WHERE p.customerId = :customerId")
    public abstract double customerPaid(String customerId);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments "
            + "WHERE paymentDate >= :from AND paymentDate <= :to")
    public abstract double paymentsBetween(long from, long to);
}
