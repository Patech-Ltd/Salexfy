package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;

import java.util.List;

@Dao
public interface StockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertMovement(StockMovement movement);

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC LIMIT :limit")
    LiveData<List<StockMovement>> observeMovementsForProduct(String productId, int limit);

    @Query("SELECT * FROM stock_movements WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    LiveData<List<StockMovement>> observeMovementsBetween(long from, long to);

    @Query("SELECT * FROM stock_movements WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    List<StockMovement> getMovementsBetween(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertStockTake(StockTake stockTake);

    @Update
    int updateStockTake(StockTake stockTake);

    @Delete
    int deleteStockTake(StockTake stockTake);

    @Query("SELECT * FROM stock_takes WHERE id = :id")
    StockTake getStockTake(String id);

    @Query("SELECT * FROM stock_takes ORDER BY stockTakeDate DESC")
    LiveData<List<StockTake>> observeStockTakes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertStockTakeItem(StockTakeItem item);

    @Query("SELECT * FROM stock_take_items WHERE stockTakeId = :stockTakeId")
    List<StockTakeItem> getStockTakeItems(String stockTakeId);

    @Query("SELECT COUNT(*) FROM stock_takes")
    int count();

    @Query("SELECT COALESCE(SUM(qty), 0) FROM stock_movements WHERE productId = :productId")
    double currentQty(String productId);

    @Query("DELETE FROM stock_movements WHERE refId = :refId AND movementType = :type")
    void deleteMovementsForRef(String refId, String type);
}
