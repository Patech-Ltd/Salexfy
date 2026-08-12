package com.patechltd.salexfypos.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.patechltd.salexfypos.db.entity.Expense;

import java.util.List;

@Dao
public interface ExpenseDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(Expense expense);

    @Query("DELETE FROM expenses WHERE id = :id")
    void delete(String id);

    @Query("SELECT * FROM expenses WHERE expenseDate >= :from AND expenseDate <= :to ORDER BY expenseDate DESC")
    List<Expense> getBetween(long from, long to);

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expenseDate >= :from AND expenseDate <= :to")
    double totalBetween(long from, long to);
}
