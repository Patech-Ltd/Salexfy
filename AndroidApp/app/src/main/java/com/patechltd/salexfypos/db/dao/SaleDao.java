package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.patechltd.salexfypos.db.CashierReportRow;
import com.patechltd.salexfypos.db.DayReportRow;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.TopProductRow;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;

import java.util.List;

@Dao
public interface SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSale(Sale sale);

    @Update
    int updateSale(Sale sale);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertSaleItem(SaleItem item);

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    void deleteItemsForSale(String saleId);

    @Query("DELETE FROM sales WHERE id = :id")
    void deleteSale(String id);

    @Query("SELECT * FROM sales WHERE id = :id")
    Sale getSale(String id);

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    List<SaleItem> getItems(String saleId);

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    SaleWithItems getSaleWithItems(String id);

    @Query("SELECT * FROM sales WHERE status = :status ORDER BY saleDate DESC")
    LiveData<List<Sale>> observeByStatus(String status);

    @Query("SELECT * FROM sales WHERE customerId = :customerId AND status != 'DRAFT' ORDER BY saleDate DESC")
    List<Sale> getSalesForCustomer(String customerId);

    @Query("SELECT * FROM sales WHERE saleDate >= :from AND saleDate <= :to ORDER BY saleDate DESC")
    LiveData<List<Sale>> observeBetween(long from, long to);

    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to ORDER BY saleDate DESC")
    LiveData<List<Sale>> observeCompleteBetween(long from, long to);

    @Transaction
    @Query("SELECT * FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' ORDER BY saleDate DESC")
    List<SaleWithItems> getCompleteWithItemsBetween(long from, long to);

    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to AND cashierId = :cashierId ORDER BY saleDate DESC")
    List<Sale> getCompleteBetween(long from, long to, String cashierId);

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY createdAt ASC")
    List<Sale> getHeld();

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY createdAt ASC")
    LiveData<List<Sale>> observeHeld();

    @Query("SELECT * FROM sales WHERE status = 'DRAFT' ORDER BY createdAt ASC")
    LiveData<List<Sale>> observeDrafts();

    @Query("SELECT COUNT(*) FROM sales WHERE status != 'DRAFT' AND status != 'HELD'")
    int countComplete();

    @Query("SELECT MAX(saleNo) FROM sales")
    String maxSaleNo();

    @Query("SELECT (saleDate / 86400000) AS dayStart, "
            + "COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM((SELECT SUM(item.qty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS totalCost, "
            + "COALESCE(SUM(total - (SELECT SUM(item.qty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit, "
            + "COALESCE(SUM(discount), 0) AS discountTotal, "
            + "COALESCE(SUM((SELECT COUNT(*) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS itemCount "
            + "FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' "
            + "GROUP BY (saleDate / 86400000) ORDER BY dayStart DESC")
    List<DayReportRow> getDailyReport(long from, long to);

    @Query("SELECT cashierId, cashierName, COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(total - (SELECT SUM(item.qty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit, "
            + "0 AS commission "
            + "FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' "
            + "GROUP BY cashierId ORDER BY totalSales DESC")
    List<CashierReportRow> getCashierReport(long from, long to);

    @Query("SELECT item.productId, COALESCE((SELECT p.name FROM products p WHERE p.id = item.productId), item.productName) AS name, "
            + "SUM(item.qty) AS totalQty, SUM(item.lineTotal) AS totalSales "
            + "FROM sale_items item JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' "
            + "GROUP BY item.productId ORDER BY totalQty DESC LIMIT :limit")
    List<TopProductRow> getTopProducts(long from, long to, int limit);

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    double salesTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(taxAmount), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    double taxTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM sales WHERE customerId = :customerId "
            + "AND status = 'COMPLETE'")
    double outstandingForCustomer(String customerId);

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    int saleCountBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(item.qty * item.costPrice), 0) FROM sale_items item "
            + "JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD'")
    double costOfSalesBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status = 'COMPLETE' AND paymentMethod = 'CREDIT'")
    double creditSalesBetween(long from, long to);
}
