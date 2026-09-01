package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_units", indices = {
        @Index(value = "productId")
})
public class ProductUnit {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    @ColumnInfo(name = "productId")
    public String productId;

    @ColumnInfo(name = "unitId")
    public String unitId;

    @ColumnInfo(name = "unitName")
    public String unitName;

    /** How many base (retail) units make 1 of this unit. 1 for the base unit. */
    public double factor = 1;

    /** Selling price for 1 of this unit. */
    public double price = 0;

    /** Optional barcode that maps directly to this unit of the product. */
    public String barcode;

    public boolean isBase = false;

    public int sortOrder = 0;
}
