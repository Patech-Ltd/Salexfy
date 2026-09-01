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
import com.patechltd.salexfypos.db.entity.PendingProduct;
import com.patechltd.salexfypos.db.entity.Product;
import com.patechltd.salexfypos.db.entity.ProductBarcode;
import com.patechltd.salexfypos.db.entity.ProductUnit;
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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertProductUnitRaw(ProductUnit unit);

    @Update
    abstract int updateProductUnitRaw(ProductUnit unit);

    @Delete
    abstract int deleteProductUnitRaw(ProductUnit unit);

    public long insertProductUnit(ProductUnit unit) {
        long id = insertProductUnitRaw(unit);
        SyncTracker.track(SyncTracker.PRODUCT_UNIT, unit.uid, "INSERT", SyncSerializer.toJson(unit));
        return id;
    }

    public int updateProductUnit(ProductUnit unit) {
        int rows = updateProductUnitRaw(unit);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PRODUCT_UNIT, unit.uid, "UPDATE", SyncSerializer.toJson(unit));
        }
        return rows;
    }

    public int deleteProductUnit(ProductUnit unit) {
        int rows = deleteProductUnitRaw(unit);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.PRODUCT_UNIT, unit.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("DELETE FROM product_units WHERE id = :id")
    public abstract void rawDeleteProductUnit(String id);

    @Query("DELETE FROM product_units WHERE productId = :productId")
    public abstract void deleteUnitsForProduct(String productId);

    @Query("SELECT * FROM product_units WHERE productId = :productId ORDER BY sortOrder ASC")
    public abstract List<ProductUnit> getUnitsByProduct(String productId);

    @Query("SELECT * FROM product_units ORDER BY productId ASC, sortOrder ASC")
    public abstract List<ProductUnit> getAllProductUnits();

    @Query("SELECT * FROM product_units WHERE unitId = :unitId")
    public abstract List<ProductUnit> getProductUnitsByUnitId(String unitId);

    @Query("UPDATE products SET retailUnit = :newName WHERE retailUnitId = :unitId")
    public abstract void renameRetailUnitForProducts(String unitId, String newName);

    @Query("UPDATE products SET wholesaleUnit = :newName WHERE wholesaleUnitId = :unitId")
    public abstract void renameWholesaleUnitForProducts(String unitId, String newName);

    @Query("UPDATE product_units SET unitName = :newName WHERE unitId = :unitId")
    public abstract void renameProductUnitsSnapshot(String unitId, String newName);

    @Query("SELECT * FROM product_units WHERE unitId = :unitId LIMIT 1")
    public abstract ProductUnit findAnyProductUnitByUnitId(String unitId);

    @Query("SELECT COUNT(*) FROM products WHERE retailUnitId = :unitId OR wholesaleUnitId = :unitId")
    public abstract int countProductsUsingUnit(String unitId);

    @Query("UPDATE products SET retailUnit = :newName WHERE retailUnitId IS NULL AND retailUnit = :oldName")
    public abstract void renameLegacyRetailUnit(String oldName, String newName);

    @Query("UPDATE products SET wholesaleUnit = :newName WHERE wholesaleUnitId IS NULL AND wholesaleUnit = :oldName")
    public abstract void renameLegacyWholesaleUnit(String oldName, String newName);

    @Query("SELECT * FROM products WHERE id = :id")
    public abstract Product getById(String id);

    // ---------- pending batch rows (temporary until committed) ----------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract long insertPending(PendingProduct pending);

    @Query("DELETE FROM pending_products WHERE id = :id")
    public abstract void deletePending(String id);

    @Query("DELETE FROM pending_products")
    public abstract void clearPending();

    @Query("SELECT * FROM pending_products ORDER BY createdAt ASC")
    public abstract List<PendingProduct> getPending();

    @Query("SELECT COUNT(*) FROM pending_products")
    public abstract int pendingCount();

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

    @Query("SELECT productId, SUM(qty) AS qty FROM stock_movements GROUP BY productId")
    public abstract List<ProductQty> getAllQtys();

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "AND isActive = 1 ORDER BY name ASC")
    public abstract LiveData<List<Product>> searchActive(String query, String categoryId);

    @Query("SELECT * FROM products WHERE (:query = '' OR name LIKE '%' || :query || '%' "
            + "OR barcode LIKE '%' || :query || '%' OR sku LIKE '%' || :query || '%' "
            + "OR id IN (SELECT productId FROM product_barcodes WHERE barcode LIKE '%' || :query || '%')) "
            + "AND (:categoryId IS NULL OR categoryId = :categoryId) "
            + "AND isActive = 1 ORDER BY name ASC LIMIT :limit OFFSET :offset")
    public abstract List<Product> searchActivePage(String query, String categoryId, int limit, int offset);

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

    @Query("SELECT p.id AS productId, p.name AS name, p.barcode AS barcode, "
            + "COALESCE(p.retailUnit, 'Pcs') AS unitLabel, "
            + "COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) AS currentQty, "
            + "p.reorderLevel AS reorderLevel, p.isActive AS isActive "
            + "FROM products p "
            + "WHERE p.isActive = 1 "
            + "AND COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) <= p.reorderLevel "
            + "ORDER BY COALESCE((SELECT SUM(m.qty) FROM stock_movements m WHERE m.productId = p.id), 0) ASC, p.name ASC")
    public abstract LiveData<List<StockRow>> observeLowStockRows();

    @Query("SELECT COUNT(*) FROM products WHERE isActive = 1")
    public abstract LiveData<Integer> observeActiveCount();

    @Query("SELECT COUNT(*) FROM products")
    public abstract int count();
}
