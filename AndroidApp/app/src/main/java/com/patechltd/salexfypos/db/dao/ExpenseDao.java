package com.patechltd.salexfypos.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.patechltd.salexfypos.db.entity.Expense;
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertRaw(Expense expense);

    @Query("DELETE FROM expenses WHERE id = :id")
    abstract void deleteRaw(String id);

    public long insert(Expense expense) {
        long id = insertRaw(expense);
        SyncTracker.track(SyncTracker.EXPENSE, expense.uid, "INSERT", SyncSerializer.toJson(expense));
        return id;
    }

    public void delete(String id) {
        deleteRaw(id);
        SyncTracker.track(SyncTracker.EXPENSE, id, "DELETE", "");
    }

    @Query("SELECT * FROM expenses WHERE expenseDate >= :from AND expenseDate <= :to ORDER BY expenseDate DESC")
    public abstract List<Expense> getBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expenseDate >= :from AND expenseDate <= :to")
    public abstract double totalBetween(long from, long to);
}
