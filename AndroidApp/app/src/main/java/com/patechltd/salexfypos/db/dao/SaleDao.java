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
import com.patechltd.salexfypos.db.MonthReportRow;
import com.patechltd.salexfypos.db.PaymentMethodTotalRow;
import com.patechltd.salexfypos.db.SaleWithItems;
import com.patechltd.salexfypos.db.TopProductRow;
import com.patechltd.salexfypos.db.entity.Sale;
import com.patechltd.salexfypos.db.entity.SaleItem;
import com.patechltd.salexfypos.db.entity.SalePayment;
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class SaleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSaleRaw(Sale sale);

    @Update
    abstract int updateSaleRaw(Sale sale);

    public long insertSale(Sale sale) {
        long id = insertSaleRaw(sale);
        SyncTracker.track(SyncTracker.SALE, sale.uid, "INSERT", SyncSerializer.toJson(sale));
        return id;
    }

    public int updateSale(Sale sale) {
        int rows = updateSaleRaw(sale);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.SALE, sale.uid, "UPDATE", SyncSerializer.toJson(sale));
        }
        return rows;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertSaleItemRaw(SaleItem item);

    public long insertSaleItem(SaleItem item) {
        long id = insertSaleItemRaw(item);
        SyncTracker.track(SyncTracker.SALE_ITEM, item.uid, "INSERT", SyncSerializer.toJson(item));
        return id;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertPaymentRaw(SalePayment payment);

    public long insertPayment(SalePayment payment) {
        long id = insertPaymentRaw(payment);
        SyncTracker.track(SyncTracker.SALE_PAYMENT, payment.uid, "INSERT", SyncSerializer.toJson(payment));
        return id;
    }

    @Query("DELETE FROM sale_payments WHERE saleId = :saleId")
    public abstract void deletePaymentsForSale(String saleId);

    @Query("DELETE FROM sale_payments WHERE id = :id")
    public abstract void rawDeletePayment(String id);

    @Query("SELECT * FROM sale_payments WHERE saleId = :saleId ORDER BY amount DESC")
    public abstract List<SalePayment> getPayments(String saleId);

    @Query("DELETE FROM sale_items WHERE saleId = :saleId")
    public abstract void deleteItemsForSale(String saleId);

    @Query("DELETE FROM sale_items WHERE id = :id")
    public abstract void rawDeleteItem(String id);

    @Query("DELETE FROM sales WHERE id = :id")
    public abstract void deleteSale(String id);

    @Query("SELECT * FROM sales WHERE id = :id")
    public abstract Sale getSale(String id);

    @Query("SELECT * FROM sale_items WHERE saleId = :saleId")
    public abstract List<SaleItem> getItems(String saleId);

    @Transaction
    @Query("SELECT * FROM sales WHERE id = :id")
    public abstract SaleWithItems getSaleWithItems(String id);

    @Query("SELECT * FROM sales WHERE status = :status ORDER BY saleDate DESC")
    public abstract LiveData<List<Sale>> observeByStatus(String status);

    @Query("SELECT * FROM sales WHERE customerId = :customerId AND status != 'DRAFT' ORDER BY saleDate DESC")
    public abstract List<Sale> getSalesForCustomer(String customerId);

    @Query("SELECT * FROM sales WHERE saleDate >= :from AND saleDate <= :to ORDER BY saleDate DESC")
    public abstract LiveData<List<Sale>> observeBetween(long from, long to);

    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to ORDER BY saleDate DESC")
    public abstract LiveData<List<Sale>> observeCompleteBetween(long from, long to);

    @Transaction
    @Query("SELECT * FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' ORDER BY saleDate DESC")
    public abstract List<SaleWithItems> getCompleteWithItemsBetween(long from, long to);

    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to AND cashierId = :cashierId ORDER BY saleDate DESC")
    public abstract List<Sale> getCompleteBetween(long from, long to, String cashierId);

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY createdAt ASC")
    public abstract List<Sale> getHeld();

    @Transaction
    @Query("SELECT * FROM sales WHERE status = 'HELD' "
            + "AND (:q = '' OR saleNo LIKE '%' || :q || '%' "
            + "     OR notes LIKE '%' || :q || '%' "
            + "     OR id IN (SELECT saleId FROM sale_items "
            + "               WHERE productName LIKE '%' || :q || '%')) "
            + "ORDER BY createdAt DESC LIMIT :limit OFFSET :offset")
    public abstract List<SaleWithItems> getHeldPage(String q, int limit, int offset);

    @Query("SELECT * FROM sales WHERE status = 'HELD' ORDER BY createdAt ASC")
    public abstract LiveData<List<Sale>> observeHeld();

    @Query("SELECT * FROM sales WHERE status = 'DRAFT' ORDER BY createdAt ASC")
    public abstract LiveData<List<Sale>> observeDrafts();

    @Query("SELECT COUNT(*) FROM sales WHERE status != 'DRAFT' AND status != 'HELD'")
    public abstract int countComplete();

    @Query("SELECT MAX(saleNo) FROM sales")
    public abstract String maxSaleNo();

    @Query("SELECT (strftime('%s', date(saleDate / 1000, 'unixepoch', 'localtime')) * 1000) AS dayStart, "
            + "COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(subtotal), 0) AS subtotal, "
            + "COALESCE(SUM((SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS totalCost, "
            + "COALESCE(SUM(total - (SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit, "
            + "COALESCE(SUM(discount), 0) AS discountTotal, "
            + "COALESCE(SUM((SELECT COUNT(*) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS itemCount "
            + "FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' "
            + "GROUP BY (strftime('%s', date(saleDate / 1000, 'unixepoch', 'localtime')) * 1000) ORDER BY dayStart DESC")
    public abstract List<DayReportRow> getDailyReport(long from, long to);

    @Query("SELECT cashierId, cashierName, COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(total - (SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit, "
            + "0 AS commission "
            + "FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' "
            + "GROUP BY cashierId ORDER BY totalSales DESC")
    public abstract List<CashierReportRow> getCashierReport(long from, long to);

    @Query("SELECT item.productId, COALESCE((SELECT p.name FROM products p WHERE p.id = item.productId), item.productName) AS name, "
            + "SUM(item.qty) AS totalQty, SUM(item.lineTotal) AS totalSales "
            + "FROM sale_items item JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' "
            + "GROUP BY item.productId ORDER BY totalQty DESC LIMIT :limit")
    public abstract List<TopProductRow> getTopProducts(long from, long to, int limit);

    @Query("SELECT (strftime('%s', date(saleDate / 1000, 'unixepoch', 'localtime', 'start of month')) * 1000) AS monthStart, "
            + "COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(subtotal), 0) AS subtotal, "
            + "COALESCE(SUM((SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS totalCost, "
            + "COALESCE(SUM(total - (SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit "
            + "FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "GROUP BY monthStart ORDER BY monthStart DESC")
    public abstract List<MonthReportRow> getMonthlySummary();

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    public abstract double salesTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(subtotal), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    public abstract double subtotalTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(taxAmount), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    public abstract double taxTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM sales WHERE customerId = :customerId "
            + "AND status = 'COMPLETE'")
    public abstract double outstandingForCustomer(String customerId);

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD'")
    public abstract int saleCountBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(item.stockQty * item.costPrice), 0) FROM sale_items item "
            + "JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD'")
    public abstract double costOfSalesBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM sales "
            + "WHERE status = 'COMPLETE' AND saleDate >= :from AND saleDate <= :to "
            + "AND (paymentMethod = 'CREDIT' "
            + "     OR id IN (SELECT saleId FROM sale_payments WHERE method = 'CREDIT'))")
    public abstract double creditSalesBetween(long from, long to);

    @Query("SELECT sp.method AS method, SUM(sp.amount) AS total "
            + "FROM sale_payments sp JOIN sales s ON s.id = sp.saleId "
            + "WHERE s.status = 'COMPLETE' AND s.saleDate >= :from AND s.saleDate <= :to "
            + "GROUP BY sp.method ORDER BY total DESC")
    public abstract List<PaymentMethodTotalRow> paymentTotalsBetween(long from, long to);

    @Transaction
    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to "
            + "AND (:q = '' OR saleNo LIKE '%' || :q || '%' "
            + "     OR customerName LIKE '%' || :q || '%' "
            + "     OR id IN (SELECT saleId FROM sale_items "
            + "               WHERE productName LIKE '%' || :q || '%' "
            + "               OR barcode LIKE '%' || :q || '%')) "
            + "ORDER BY saleDate DESC LIMIT :limit OFFSET :offset")
    public abstract List<SaleWithItems> searchCompletePage(String q, long from, long to, int limit, int offset);

    @Transaction
    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to "
            + "AND (:q = '' OR saleNo LIKE '%' || :q || '%' "
            + "     OR customerName LIKE '%' || :q || '%' "
            + "     OR id IN (SELECT saleId FROM sale_items "
            + "               WHERE productName LIKE '%' || :q || '%' "
            + "               OR barcode LIKE '%' || :q || '%')) "
            + "ORDER BY saleDate DESC")
    public abstract List<SaleWithItems> exportComplete(String q, long from, long to);
}
