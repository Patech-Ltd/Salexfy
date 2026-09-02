package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.patechltd.salexfypos.db.BillRow;
import com.patechltd.salexfypos.db.PurchaseWithItems;
import com.patechltd.salexfypos.db.SupplierPayableRow;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;
import com.patechltd.salexfypos.db.entity.SupplierPayment;
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertPurchaseRaw(Purchase purchase);

    @Update
    abstract int updatePurchaseRaw(Purchase purchase);

    @Delete
    abstract int deletePurchaseRaw(Purchase purchase);

    public long insertPurchase(Purchase purchase) {
        long id = insertPurchaseRaw(purchase);
        SyncTracker.track(SyncTracker.PURCHASE, purchase.uid, "INSERT", SyncSerializer.toJson(purchase));
        return id;
    }

    public int updatePurchase(Purchase purchase) {
        int rows = updatePurchaseRaw(purchase);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PURCHASE, purchase.uid, "UPDATE", SyncSerializer.toJson(purchase));
        }
        return rows;
    }

    public int deletePurchase(Purchase purchase) {
        int rows = deletePurchaseRaw(purchase);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PURCHASE, purchase.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT MAX(invoiceNo) FROM purchases")
    public abstract String maxPurchaseInvoice();

    @Query("SELECT * FROM purchases WHERE id = :id")
    public abstract Purchase getPurchase(String id);

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    public abstract LiveData<List<Purchase>> observePurchases();

    @Query("SELECT * FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to ORDER BY purchaseDate DESC")
    public abstract LiveData<List<Purchase>> observePurchasesBetween(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertPurchaseItemRaw(PurchaseItem item);

    @Update
    abstract int updatePurchaseItemRaw(PurchaseItem item);

    public long insertPurchaseItem(PurchaseItem item) {
        long id = insertPurchaseItemRaw(item);
        SyncTracker.track(SyncTracker.PURCHASE_ITEM, item.uid, "INSERT", SyncSerializer.toJson(item));
        return id;
    }

    public int updatePurchaseItem(PurchaseItem item) {
        int rows = updatePurchaseItemRaw(item);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PURCHASE_ITEM, item.uid, "UPDATE", SyncSerializer.toJson(item));
        }
        return rows;
    }

    @Query("DELETE FROM purchase_items WHERE id = :id")
    public abstract void rawDeleteItem(String id);

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    public abstract void deleteItemsForPurchase(String purchaseId);

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    public abstract List<PurchaseItem> getItems(String purchaseId);

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :id")
    public abstract PurchaseWithItems getPurchaseWithItems(String id);

    @Transaction
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    public abstract List<PurchaseWithItems> getPurchasesWithItems();

    @Transaction
    @Query("SELECT * FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to ORDER BY purchaseDate DESC")
    public abstract List<PurchaseWithItems> getPurchasesWithItemsBetween(long from, long to);

    @Query("SELECT COUNT(*) FROM purchases WHERE invoiceNo = :invoiceNo AND id != :excludeId")
    public abstract int countInvoice(String invoiceNo, String excludeId);

    @Query("SELECT COALESCE(SUM(total), 0) FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to")
    public abstract double purchasesTotal(long from, long to);

    @Query("SELECT COUNT(*) FROM purchases")
    public abstract int count();

    @Query("SELECT su.id AS supplierId, su.name, su.phone, "
            + "COALESCE((SELECT SUM(p.total - p.paidAmount) FROM purchases p WHERE p.supplierId = su.id), 0) AS payable "
            + "FROM suppliers su ORDER BY su.name ASC")
    public abstract List<SupplierPayableRow> getSupplierPayables();

    @Query("SELECT p.id AS purchaseId, p.invoiceNo, p.supplierId, su.name AS supplierName, "
            + "p.purchaseDate, p.total, p.paidAmount, (p.total - p.paidAmount) AS balance "
            + "FROM purchases p LEFT JOIN suppliers su ON su.id = p.supplierId "
            + "WHERE (p.total - p.paidAmount) > 0.001 "
            + "ORDER BY p.purchaseDate ASC")
    public abstract List<BillRow> getBills();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSupplierPaymentRaw(SupplierPayment payment);

    public long insertSupplierPayment(SupplierPayment payment) {
        long id = insertSupplierPaymentRaw(payment);
        SyncTracker.track(SyncTracker.SUPPLIER_PAYMENT, payment.uid, "INSERT", SyncSerializer.toJson(payment));
        return id;
    }

    @Query("SELECT * FROM supplier_payments WHERE purchaseId = :purchaseId ORDER BY paymentDate DESC")
    public abstract List<SupplierPayment> getSupplierPayments(String purchaseId);

    @Query("DELETE FROM supplier_payments WHERE id = :id")
    public abstract void rawDeleteSupplierPayment(String id);

    @Query("SELECT COALESCE(SUM(p.total - p.paidAmount), 0) FROM purchases p")
    public abstract double totalOutstanding();
}
