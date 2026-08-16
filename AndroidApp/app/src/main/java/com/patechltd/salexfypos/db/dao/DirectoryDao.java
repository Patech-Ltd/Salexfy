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
import com.patechltd.salexfypos.sync.SyncSerializer;
import com.patechltd.salexfypos.sync.SyncTracker;

import java.util.List;

@Dao
public abstract class DirectoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertCategoryRaw(Category category);

    @Update
    abstract int updateCategoryRaw(Category category);

    @Delete
    abstract int deleteCategoryRaw(Category category);

    public long insertCategory(Category category) {
        long id = insertCategoryRaw(category);
        SyncTracker.track(SyncTracker.CATEGORY, category.uid, "INSERT", SyncSerializer.toJson(category));
        return id;
    }

    public int updateCategory(Category category) {
        int rows = updateCategoryRaw(category);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.CATEGORY, category.uid, "UPDATE", SyncSerializer.toJson(category));
        }
        return rows;
    }

    public int deleteCategory(Category category) {
        int rows = deleteCategoryRaw(category);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.CATEGORY, category.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("DELETE FROM categories WHERE id = :id")
    public abstract void rawDeleteCategory(String id);

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    public abstract LiveData<List<Category>> observeCategories();

    @Query("SELECT * FROM categories ORDER BY sortOrder ASC, name ASC")
    public abstract List<Category> getCategories();

    @Query("SELECT COUNT(*) FROM categories")
    public abstract int categoryCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertBrandRaw(Brand brand);

    @Update
    abstract int updateBrandRaw(Brand brand);

    @Delete
    abstract int deleteBrandRaw(Brand brand);

    public long insertBrand(Brand brand) {
        long id = insertBrandRaw(brand);
        SyncTracker.track(SyncTracker.BRAND, brand.uid, "INSERT", SyncSerializer.toJson(brand));
        return id;
    }

    public int updateBrand(Brand brand) {
        int rows = updateBrandRaw(brand);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.BRAND, brand.uid, "UPDATE", SyncSerializer.toJson(brand));
        }
        return rows;
    }

    public int deleteBrand(Brand brand) {
        int rows = deleteBrandRaw(brand);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.BRAND, brand.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("DELETE FROM brands WHERE id = :id")
    public abstract void rawDeleteBrand(String id);

    @Query("SELECT * FROM brands ORDER BY name ASC")
    public abstract LiveData<List<Brand>> observeBrands();

    @Query("SELECT * FROM brands ORDER BY name ASC")
    public abstract List<Brand> getBrands();

    @Query("SELECT COUNT(*) FROM brands")
    public abstract int brandCount();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long insertUnitRaw(Unit unit);

    @Update
    abstract int updateUnitRaw(Unit unit);

    @Delete
    abstract int deleteUnitRaw(Unit unit);

    public long insertUnit(Unit unit) {
        long id = insertUnitRaw(unit);
        SyncTracker.track(SyncTracker.UNIT, unit.uid, "INSERT", SyncSerializer.toJson(unit));
        return id;
    }

    public int updateUnit(Unit unit) {
        int rows = updateUnitRaw(unit);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.UNIT, unit.uid, "UPDATE", SyncSerializer.toJson(unit));
        }
        return rows;
    }

    public int deleteUnit(Unit unit) {
        int rows = deleteUnitRaw(unit);
        if (rows > 0) {
            SyncTracker.track(SyncTracker.UNIT, unit.uid, "DELETE", "");
        }
        return rows;
    }

    @Query("DELETE FROM units WHERE id = :id")
    public abstract void rawDeleteUnit(String id);

    @Query("SELECT * FROM units ORDER BY name ASC")
    public abstract LiveData<List<Unit>> observeUnits();

    @Query("SELECT * FROM units ORDER BY name ASC")
    public abstract List<Unit> getUnits();

    @Query("SELECT COUNT(*) FROM units")
    public abstract int unitCount();
}
