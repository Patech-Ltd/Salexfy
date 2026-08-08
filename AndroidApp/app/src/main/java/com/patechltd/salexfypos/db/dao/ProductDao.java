package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.ProductStock;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;

import java.util.List;

@Dao
public interface ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Product product);

    @Update
    int update(Product product);

    @Delete
    int delete(Product product);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertBarcode(ProductBarcode barcode);

    @Delete
    int deleteBarcode(ProductBarcode barcode);

    @Query("DELETE FROM product_barcodes WHERE productId = :productId")
    void deleteBarcodesForProduct(String productId);

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId")
    List<ProductBarcode> getBarcodesByProduct(String productId);

    @Query("SELECT * FROM product_barcodes")
    List<ProductBarcode> getAllBarcodes();

    @Query("SELECT * FROM products WHERE id = :id")
    Product getById(String id);

    @Query("SELECT * FROM products WHERE barcode = :barcode "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode = :barcode) LIMIT 1")
    Product findByBarcode(String barcode);

    @Query("SELECT * FROM products WHERE (barcode = :barcode "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode = :barcode)) "
            + "AND isActive = 1 LIMIT 1")
    Product findActiveByBarcode(String barcode);

    @Query("SELECT * FROM products ORDER BY name ASC")
    LiveData<List<Product>> observeAll();

    @Query("SELECT * FROM products ORDER BY name ASC")
    List<Product> getAll();

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    List<Product> getAllActive();

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "ORDER BY name ASC")
    LiveData<List<Product>> search(String query, String categoryId);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "ORDER BY name ASC LIMIT :limit")
    List<Product> searchTop(String query, String categoryId, int limit);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "AND isActive = 1 ORDER BY name ASC")
    LiveData<List<Product>> searchActive(String query, String categoryId);

    @Query("SELECT p.id as productId, p.name, p.barcode, COALESCE(p.retailUnit,'Pcs') as unitLabel, "
            + "COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) as currentQty, "
            + "p.reorderLevel, p.isActive "
            + "FROM products p "
            + "WHERE (:query = '' OR p.name LIKE '%' || :query || '%' OR p.barcode LIKE '%' || :query || '%' "
            + "OR p.id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "ORDER BY p.name ASC")
    LiveData<List<com.patechltd.salexfypos.db.StockRow>> observeStockRows(String query);

    @Query("SELECT COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = :productId), 0) AS currentQty")
    double getCurrentQty(String productId);

    @Query("SELECT p.* FROM products p "
            + "WHERE p.isActive = 1 "
            + "AND COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) <= p.reorderLevel "
            + "ORDER BY p.name ASC")
    LiveData<List<Product>> observeLowStock();

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    LiveData<Integer> observeActiveCount();

    @Query("SELECT COUNT(*) FROM products")
    int count();
}
