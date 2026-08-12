package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "products", indices = {
        @Index(value = "barcode"),
        @Index(value = "categoryId"),
        @Index(value = "brandId"),
        @Index(value = "name")
})
public class Product {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String name;

    public String barcode;

    public String sku;

    public String imagePath;

    public String categoryId;

    public String brandId;

    public String retailUnit = "Pcs";

    public String wholesaleUnit = "Carton";

    public int wholesaleFactor = 1;

    public double retailPrice = 0;

    public double wholesalePrice = 0;

    public double costPrice = 0;

    public double reorderLevel = 0;

    public double taxPercent = 0;

    public String notes;

    public boolean isActive = true;

    public long createdAt;

    public long updatedAt;
}
