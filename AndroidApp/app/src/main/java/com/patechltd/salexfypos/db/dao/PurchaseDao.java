package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.patechltd.salexfypos.db.PurchaseWithItems;
import com.patechltd.salexfypos.db.entity.Purchase;
import com.patechltd.salexfypos.db.entity.PurchaseItem;

import java.util.List;

@Dao
public interface PurchaseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPurchase(Purchase purchase);

    @Update
    int updatePurchase(Purchase purchase);

    @Delete
    int deletePurchase(Purchase purchase);

    @Query("SELECT MAX(invoiceNo) FROM purchases")
    String maxPurchaseInvoice();

    @Query("SELECT * FROM purchases WHERE id = :id")
    Purchase getPurchase(String id);

    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    LiveData<List<Purchase>> observePurchases();

    @Query("SELECT * FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to ORDER BY purchaseDate DESC")
    LiveData<List<Purchase>> observePurchasesBetween(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPurchaseItem(PurchaseItem item);

    @Update
    int updatePurchaseItem(PurchaseItem item);

    @Query("DELETE FROM purchase_items WHERE purchaseId = :purchaseId")
    void deleteItemsForPurchase(String purchaseId);

    @Query("SELECT * FROM purchase_items WHERE purchaseId = :purchaseId")
    List<PurchaseItem> getItems(String purchaseId);

    @Transaction
    @Query("SELECT * FROM purchases WHERE id = :id")
    PurchaseWithItems getPurchaseWithItems(String id);

    @Transaction
    @Query("SELECT * FROM purchases ORDER BY purchaseDate DESC")
    List<PurchaseWithItems> getPurchasesWithItems();

    @Transaction
    @Query("SELECT * FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to ORDER BY purchaseDate DESC")
    List<PurchaseWithItems> getPurchasesWithItemsBetween(long from, long to);

    @Query("SELECT COUNT(*) FROM purchases WHERE invoiceNo = :invoiceNo AND id != :excludeId")
    int countInvoice(String invoiceNo, String excludeId);

    @Query("SELECT COALESCE(SUM(total), 0) FROM purchases WHERE purchaseDate >= :from AND purchaseDate <= :to")
    double purchasesTotal(long from, long to);

    @Query("SELECT COUNT(*) FROM purchases")
    int count();
}
