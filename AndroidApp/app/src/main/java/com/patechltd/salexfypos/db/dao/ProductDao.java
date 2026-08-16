package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.ProductQty;
import com.patechltd.salexfypos.db.ProductStock;
import com.patechltd.salexfypos.db.StockRow;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class ProductDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertRaw(Product product);

    @Update
    abstract int updateRaw(Product product);

    @Delete
    abstract int deleteRaw(Product product);

    public long insert(Product product) {
        long id = insertRaw(product);
        SyncTracker.track(SyncTracker.PRODUCT, product.uid, "INSERT", SyncSerializer.toJson(product));
        return id;
    }

    public int update(Product product) {
        int rows = updateRaw(product);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PRODUCT, product.uid, "UPDATE", SyncSerializer.toJson(product));
        }
        return rows;
    }

    public int delete(Product product) {
        int rows = deleteRaw(product);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PRODUCT, product.uid, "DELETE", "");
        }
        return rows;
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract void insertBarcodeRaw(ProductBarcode barcode);

    @Delete
    abstract int deleteBarcodeRaw(ProductBarcode barcode);

    public void insertBarcode(ProductBarcode barcode) {
        insertBarcodeRaw(barcode);
        SyncTracker.track(SyncTracker.PRODUCT_BARCODE, barcode.uid, "INSERT", SyncSerializer.toJson(barcode));
    }

    public int deleteBarcode(ProductBarcode barcode) {
        int rows = deleteBarcodeRaw(barcode);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PRODUCT_BARCODE, barcode.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("DELETE FROM product_barcodes WHERE id = :id")
    public abstract void rawDeleteBarcode(String id);

    @Query("DELETE FROM product_barcodes WHERE productId = :productId")
    public abstract void deleteBarcodesForProduct(String productId);

    @Query("SELECT * FROM product_barcodes WHERE productId = :productId")
    public abstract List<ProductBarcode> getBarcodesByProduct(String productId);

    @Query("SELECT * FROM product_barcodes")
    public abstract List<ProductBarcode> getAllBarcodes();

    @Query("SELECT * FROM products WHERE id = :id")
    public abstract Product getById(String id);

    @Query("SELECT * FROM products WHERE barcode = :barcode "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode = :barcode) LIMIT 1")
    public abstract Product findByBarcode(String barcode);

    @Query("SELECT * FROM products WHERE (barcode = :barcode "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode = :barcode)) "
            + "AND isActive = 1 LIMIT 1")
    public abstract Product findActiveByBarcode(String barcode);

    @Query("SELECT * FROM products ORDER BY name ASC")
    public abstract LiveData<List<Product>> observeAll();

    @Query("SELECT * FROM products ORDER BY name ASC")
    public abstract List<Product> getAll();

    @Query("SELECT * FROM products WHERE isActive = 1 ORDER BY name ASC")
    public abstract List<Product> getAllActive();

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "ORDER BY name ASC")
    public abstract LiveData<List<Product>> search(String query, String categoryId);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "ORDER BY name ASC LIMIT :limit")
    public abstract List<Product> searchTop(String query, String categoryId, int limit);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "ORDER BY name ASC LIMIT :limit OFFSET :offset")
    public abstract List<Product> searchPage(String query, String categoryId, int limit, int offset);

    @Query("SELECT productId, SUM(qty) AS qty FROM stock_movements "
            + "WHERE productId IN (:ids) GROUP BY productId")
    public abstract List<ProductQty> getQtys(List<String> ids);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "AND isActive = 1 ORDER BY name ASC")
    public abstract LiveData<List<Product>> searchActive(String query, String categoryId);

    @Query("SELECT p.id as productId, p.name, p.barcode, COALESCE(p.retailUnit,'Pcs') as unitLabel, "
            + "COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) as currentQty, "
            + "p.reorderLevel, p.isActive "
            + "FROM products p "
            + "WHERE (:query = '' OR p.name LIKE '%' || :query || '%' OR p.barcode LIKE '%' || :query || '%' "
            + "OR p.id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "ORDER BY p.name ASC")
    public abstract LiveData<List<StockRow>> observeStockRows(String query);

    @Query("SELECT COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = :productId), 0) AS currentQty")
    public abstract double getCurrentQty(String productId);

    @Query("SELECT p.* FROM products p "
            + "WHERE p.isActive = 1 "
            + "AND COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) <= p.reorderLevel "
            + "ORDER BY p.name ASC")
    public abstract LiveData<List<Product>> observeLowStock();

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    public abstract LiveData<Integer> observeActiveCount();

    @Query("SELECT COUNT(*) FROM products")
    public abstract int count();
}
