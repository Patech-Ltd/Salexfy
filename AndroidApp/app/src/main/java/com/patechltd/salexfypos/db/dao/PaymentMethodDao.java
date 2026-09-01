package com.patechltd.salexfypos.db.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.entity.PaymentMethod;

import java.util.List;

@Dao
public abstract class PaymentMethodDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insert(PaymentMethod method);

    @Update
    public abstract void update(PaymentMethod method);

    @Delete
    public abstract void delete(PaymentMethod method);

    @Query("SELECT * FROM payment_methods ORDER BY sortOrder ASC, name ASC")
    public abstract List<PaymentMethod> getAll();

    @Query("SELECT * FROM payment_methods WHERE active = 1 ORDER BY sortOrder ASC, name ASC")
    public abstract List<PaymentMethod> getActive();

    @Query("SELECT * FROM payment_methods WHERE id = :id LIMIT 1")
    public abstract PaymentMethod getById(String id);
}