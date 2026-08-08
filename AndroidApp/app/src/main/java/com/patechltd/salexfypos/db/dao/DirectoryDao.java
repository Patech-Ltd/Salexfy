package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.patechltd.salexfypos.db.entity.Brand;
import com.patechltd.salexfypos.db.entity.Category;
import com.patechltd.salexfypos.db.entity.Unit;

import java.util.List;

@Dao
public interface DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCategory(Category category);

    @Update
    int updateCategory(Category category);

    @Delete
    int deleteCategory(Category category);

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    LiveData<List<Category>> observeCategories();

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    List<Category> getCategories();

    @Query("SELECT COUNT(*) FROM categories")
    int categoryCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBrand(Brand brand);

    @Update
    int updateBrand(Brand brand);

    @Delete
    int deleteBrand(Brand brand);

    @Query("SELECT * FROM brands ORDER BY name ASC")
    LiveData<List<Brand>> observeBrands();

    @Query("SELECT * FROM brands ORDER BY name ASC")
    List<Brand> getBrands();

    @Query("SELECT COUNT(*) FROM brands")
    int brandCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertUnit(Unit unit);

    @Update
    int updateUnit(Unit unit);

    @Delete
    int deleteUnit(Unit unit);

    @Query("SELECT * FROM units ORDER BY name ASC")
    LiveData<List<Unit>> observeUnits();

    @Query("SELECT * FROM units ORDER BY name ASC")
    List<Unit> getUnits();

    @Query("SELECT COUNT(*) FROM units")
    int unitCount();
}
