package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.entity.StockMovement;
import com.patechltd.salexfypos.db.entity.StockTake;
import com.patechltd.salexfypos.db.entity.StockTakeItem;
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class StockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertMovementRaw(StockMovement movement);

    public long insertMovement(StockMovement movement) {
        long id = insertMovementRaw(movement);
        SyncTracker.track(SyncTracker.STOCK_MOVEMENT, movement.uid, "INSERT", SyncSerializer.toJson(movement));
        return id;
    }

    @Query("DELETE FROM stock_movements WHERE id = :id")
    public abstract void rawDeleteMovement(String id);

    @Query("SELECT * FROM stock_movements WHERE productId = :productId ORDER BY createdAt DESC LIMIT :limit")
    public abstract LiveData<List<StockMovement>> observeMovementsForProduct(String productId, int limit);

    @Query("SELECT COUNT(*) FROM stock_movements WHERE productId = :productId")
    public abstract int countMovementsForProduct(String productId);

    /** Keyset-paginated fetch (runs on a worker thread, not the UI thread). Pass
     *  beforeCreatedAt == 0 for the newest page; afterwards pass the last seen
     *  row's createdAt + id so deep pages stay O(log n). */
    @Query("SELECT * FROM stock_movements "
            + "WHERE productId = :productId "
            + "AND (:beforeCreatedAt = 0 OR createdAt < :beforeCreatedAt "
            + "OR (createdAt = :beforeCreatedAt AND id < :beforeId)) "
            + "ORDER BY createdAt DESC, id DESC LIMIT :limit")
    public abstract List<StockMovement> getMovementsPage(String productId,
                                                         long beforeCreatedAt,
                                                         String beforeId,
                                                         int limit);

    @Query("SELECT * FROM stock_movements WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    public abstract LiveData<List<StockMovement>> observeMovementsBetween(long from, long to);

    @Query("SELECT * FROM stock_movements WHERE createdAt >= :from AND createdAt <= :to ORDER BY createdAt DESC")
    public abstract List<StockMovement> getMovementsBetween(long from, long to);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertStockTakeRaw(StockTake stockTake);

    @Update
    abstract int updateStockTakeRaw(StockTake stockTake);

    @Delete
    abstract int deleteStockTakeRaw(StockTake stockTake);

    public long insertStockTake(StockTake stockTake) {
        long id = insertStockTakeRaw(stockTake);
        SyncTracker.track(SyncTracker.STOCK_TAKE, stockTake.uid, "INSERT", SyncSerializer.toJson(stockTake));
        return id;
    }

    public int updateStockTake(StockTake stockTake) {
        int rows = updateStockTakeRaw(stockTake);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.STOCK_TAKE, stockTake.uid, "UPDATE", SyncSerializer.toJson(stockTake));
        }
        return rows;
    }

    public int deleteStockTake(StockTake stockTake) {
        int rows = deleteStockTakeRaw(stockTake);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.STOCK_TAKE, stockTake.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("SELECT * FROM stock_takes WHERE id = :id")
    public abstract StockTake getStockTake(String id);

    @Query("SELECT * FROM stock_takes ORDER BY stockTakeDate DESC")
    public abstract LiveData<List<StockTake>> observeStockTakes();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertStockTakeItemRaw(StockTakeItem item);

    public long insertStockTakeItem(StockTakeItem item) {
        long id = insertStockTakeItemRaw(item);
        return id;
    }

    @Query("SELECT * FROM stock_take_items WHERE stockTakeId = :stockTakeId")
    public abstract List<StockTakeItem> getStockTakeItems(String stockTakeId);

    @Query("SELECT COUNT(*) FROM stock_takes")
    public abstract int count();

    @Query("SELECT COALESCE(SUM(qty), 0) FROM stock_movements WHERE productId = :productId")
    public abstract double currentQty(String productId);

    @Query("DELETE FROM stock_movements WHERE refId = :refId AND movementType = :type")
    public abstract void deleteMovementsForRef(String refId, String type);
}
