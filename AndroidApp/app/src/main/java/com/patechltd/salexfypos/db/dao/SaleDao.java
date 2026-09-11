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
import com.patechltd.salexfypos.db.DeletedSaleItemRow;
import com.patechltd.salexfypos.db.MonthReportRow;
import com.patechltd.salexfypos.db.PaymentMethodTotalRow;
import com.patechltd.salexfypos.db.ProductSalesRow;
import com.patechltd.salexfypos.db.ProductSalesSummaryRow;
import com.patechltd.salexfypos.db.ProfitLineRow;
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
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID' "
            + "GROUP BY (strftime('%s', date(saleDate / 1000, 'unixepoch', 'localtime')) * 1000) ORDER BY dayStart DESC")
    public abstract List<DayReportRow> getDailyReport(long from, long to);

    @Query("SELECT cashierId, cashierName, COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(total - (SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit, "
            + "0 AS commission "
            + "FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID' "
            + "GROUP BY cashierId ORDER BY totalSales DESC")
    public abstract List<CashierReportRow> getCashierReport(long from, long to);

    @Query("SELECT item.productId, COALESCE((SELECT p.name FROM products p WHERE p.id = item.productId), item.productName) AS name, "
            + "SUM(item.qty) AS totalQty, SUM(item.lineTotal) AS totalSales "
            + "FROM sale_items item JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "GROUP BY item.productId ORDER BY totalQty DESC LIMIT :limit")
    public abstract List<TopProductRow> getTopProducts(long from, long to, int limit);

    @Query("SELECT si.productId AS productId, "
            + "COALESCE((SELECT p.name FROM products p WHERE p.id = si.productId), si.productName) AS name, "
            + "COALESCE((SELECT p.retailUnit FROM products p WHERE p.id = si.productId), si.unitLabel, 'Pcs') AS unitLabel, "
            + "SUM(si.qty) AS qty, "
            + "SUM(si.stockQty) AS stockQty, "
            + "SUM(si.lineTotal) AS revenue, "
            + "SUM(si.stockQty * si.costPrice) AS cost, "
            + "SUM(si.lineTotal) - SUM(si.stockQty * si.costPrice) AS profit, "
            + "COUNT(DISTINCT s.id) AS saleCount "
            + "FROM sale_items si JOIN sales s ON s.id = si.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:productId IS NULL OR si.productId = :productId) "
            + "AND (:customerId IS NULL OR s.customerId = :customerId) "
            + "GROUP BY si.productId "
            + "ORDER BY name ASC LIMIT :limit OFFSET :offset")
    public abstract List<ProductSalesRow> getProductSales(long from, long to,
                                                          String productId, String customerId,
                                                          int limit, int offset);

    @Query("SELECT COUNT(DISTINCT si.productId) FROM sale_items si JOIN sales s ON s.id = si.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:productId IS NULL OR si.productId = :productId) "
            + "AND (:customerId IS NULL OR s.customerId = :customerId)")
    public abstract int countProductSales(long from, long to, String productId, String customerId);

    @Query("SELECT COALESCE(SUM(si.qty), 0) AS qty, "
            + "COALESCE(SUM(si.stockQty), 0) AS stockQty, "
            + "COALESCE(SUM(si.lineTotal), 0) AS revenue, "
            + "COALESCE(SUM(si.stockQty * si.costPrice), 0) AS cost, "
            + "COUNT(DISTINCT s.id) AS saleCount "
            + "FROM sale_items si JOIN sales s ON s.id = si.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:productId IS NULL OR si.productId = :productId) "
            + "AND (:customerId IS NULL OR s.customerId = :customerId)")
    public abstract ProductSalesSummaryRow getProductSalesSummary(long from, long to,
                                                                  String productId, String customerId);

    @Query("SELECT (strftime('%s', date(saleDate / 1000, 'unixepoch', 'localtime', 'start of month')) * 1000) AS monthStart, "
            + "COUNT(*) AS saleCount, "
            + "COALESCE(SUM(total), 0) AS totalSales, "
            + "COALESCE(SUM(subtotal), 0) AS subtotal, "
            + "COALESCE(SUM((SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS totalCost, "
            + "COALESCE(SUM(total - (SELECT SUM(item.stockQty * item.costPrice) FROM sale_items item WHERE item.saleId = sales.id)), 0) AS profit "
            + "FROM sales WHERE status != 'DRAFT' AND status != 'HELD' AND status != 'VOID' "
            + "GROUP BY monthStart ORDER BY monthStart DESC")
    public abstract List<MonthReportRow> getMonthlySummary();

    @Query("SELECT COALESCE(SUM(total), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID'")
    public abstract double salesTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(subtotal), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID'")
    public abstract double subtotalTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(taxAmount), 0) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID'")
    public abstract double taxTotal(long from, long to);

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM sales WHERE customerId = :customerId "
            + "AND status = 'COMPLETE'")
    public abstract double outstandingForCustomer(String customerId);

    @Query("SELECT COUNT(*) FROM sales WHERE saleDate >= :from AND saleDate <= :to "
            + "AND status != 'DRAFT' AND status != 'HELD' AND status != 'VOID'")
    public abstract int saleCountBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(item.stockQty * item.costPrice), 0) FROM sale_items item "
            + "JOIN sales s ON s.id = item.saleId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID'")
    public abstract double costOfSalesBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(total - paidAmount), 0) FROM sales "
            + "WHERE status = 'COMPLETE' AND saleDate >= :from AND saleDate <= :to "
            + "AND (paymentMethod = 'CREDIT' "
            + "     OR id IN (SELECT saleId FROM sale_payments WHERE method = 'CREDIT'))")
    public abstract double creditSalesBetween(long from, long to);

    @Query("SELECT sp.method AS method, SUM(sp.amount) AS total "
            + "FROM sale_payments sp JOIN sales s ON s.id = sp.saleId "
            + "WHERE s.status != 'VOID' AND s.saleDate >= :from AND s.saleDate <= :to "
            + "GROUP BY sp.method ORDER BY total DESC")
    public abstract List<PaymentMethodTotalRow> paymentTotalsBetween(long from, long to);

    @Transaction
    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to "
            + "AND (:deletedFilter = 0 OR id IN (SELECT si.saleId FROM sale_items si "
            + "     LEFT JOIN products p ON p.id = si.productId "
            + "     WHERE p.id IS NULL OR p.isActive = 0)) "
            + "AND (:filter = 0 OR (:filter = 1 AND id IN (SELECT saleId FROM sale_items WHERE isWholesale = 1)) "
            + "          OR (:filter = 2 AND id NOT IN (SELECT saleId FROM sale_items WHERE isWholesale = 1))) "
            + "AND (:q = '' OR saleNo LIKE '%' || :q || '%' "
            + "     OR customerName LIKE '%' || :q || '%' "
            + "     OR id IN (SELECT saleId FROM sale_items "
            + "               WHERE productName LIKE '%' || :q || '%' "
            + "               OR barcode LIKE '%' || :q || '%')) "
            + "ORDER BY saleDate DESC LIMIT :limit OFFSET :offset")
    public abstract List<SaleWithItems> searchCompletePage(String q, long from, long to, int filter,
                                                           int deletedFilter, int limit, int offset);

    @Transaction
    @Query("SELECT * FROM sales WHERE status != 'DRAFT' AND status != 'HELD' "
            + "AND saleDate >= :from AND saleDate <= :to "
            + "AND (:deletedFilter = 0 OR id IN (SELECT si.saleId FROM sale_items si "
            + "     LEFT JOIN products p ON p.id = si.productId "
            + "     WHERE p.id IS NULL OR p.isActive = 0)) "
            + "AND (:filter = 0 OR (:filter = 1 AND id IN (SELECT saleId FROM sale_items WHERE isWholesale = 1)) "
            + "          OR (:filter = 2 AND id NOT IN (SELECT saleId FROM sale_items WHERE isWholesale = 1))) "
            + "AND (:q = '' OR saleNo LIKE '%' || :q || '%' "
            + "     OR customerName LIKE '%' || :q || '%' "
            + "     OR id IN (SELECT saleId FROM sale_items "
            + "               WHERE productName LIKE '%' || :q || '%' "
            + "               OR barcode LIKE '%' || :q || '%')) "
            + "ORDER BY saleDate DESC")
    public abstract List<SaleWithItems> exportComplete(String q, long from, long to, int filter, int deletedFilter);

    @Query("SELECT DISTINCT si.saleId FROM sale_items si "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE p.id IS NULL OR p.isActive = 0")
    public abstract List<String> saleIdsWithDeletedItems();

    @Query("SELECT s.id AS saleUid, s.saleNo, s.saleDate, s.status, "
            + "si.productName, si.unitLabel, si.qty, si.stockQty, si.unitPrice, si.costPrice, "
            + "si.isWholesale AS isWholesale, "
            + "s.cashierName AS cashierName, s.customerName AS customerName, s.paymentMethod AS paymentMethod, "
            + "(p.id IS NULL OR p.isActive = 0) AS productDeleted, "
            + "si.qty * si.unitPrice AS revenue, si.stockQty * si.costPrice AS cost, "
            + "(si.qty * si.unitPrice) - (si.stockQty * si.costPrice) AS profit "
            + "FROM sales s JOIN sale_items si ON s.id = si.saleId "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:showDeleted = 1 OR (p.id IS NOT NULL AND p.isActive = 1)) "
            + "ORDER BY s.saleDate DESC, s.id LIMIT :limit OFFSET :offset")
    public abstract List<ProfitLineRow> getProfitLines(long from, long to, boolean showDeleted,
                                                       int limit, int offset);

    @Query("SELECT COUNT(*) FROM sale_items si "
            + "JOIN sales s ON s.id = si.saleId "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:showDeleted = 1 OR (p.id IS NOT NULL AND p.isActive = 1))")
    public abstract int profitLineCount(long from, long to, boolean showDeleted);

    @Query("SELECT COALESCE(SUM(si.qty * si.unitPrice), 0) FROM sale_items si "
            + "JOIN sales s ON s.id = si.saleId "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:showDeleted = 1 OR (p.id IS NOT NULL AND p.isActive = 1))")
    public abstract double getSummaryRevenue(long from, long to, boolean showDeleted);

    @Query("SELECT COALESCE(SUM(si.stockQty * si.costPrice), 0) FROM sale_items si "
            + "JOIN sales s ON s.id = si.saleId "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE s.saleDate >= :from AND s.saleDate <= :to "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' AND s.status != 'VOID' "
            + "AND (:showDeleted = 1 OR (p.id IS NOT NULL AND p.isActive = 1))")
    public abstract double getSummaryCost(long from, long to, boolean showDeleted);

    @Query("SELECT si.id AS saleItemId, si.saleId, s.saleNo, s.saleDate, s.status AS saleStatus, "
            + "si.productId, si.productName, si.qty, si.unitPrice, si.lineTotal, si.unitLabel, "
            + "si.isWholesale, s.cashierName "
            + "FROM sale_items si "
            + "JOIN sales s ON s.id = si.saleId "
            + "LEFT JOIN products p ON p.id = si.productId "
            + "WHERE (p.id IS NULL OR p.isActive = 0) "
            + "AND s.status != 'DRAFT' AND s.status != 'HELD' "
            + "ORDER BY s.saleDate DESC")
    public abstract List<DeletedSaleItemRow> getDeletedSaleItems();

    @Query("DELETE FROM sale_items WHERE id = :itemId")
    public abstract void deleteSaleItemById(String itemId);

    @Query("SELECT saleId FROM sale_items WHERE id = :itemId")
    public abstract String getSaleIdForItem(String itemId);
}
