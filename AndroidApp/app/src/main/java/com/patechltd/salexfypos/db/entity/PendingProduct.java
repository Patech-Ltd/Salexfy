package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * A product row captured during batch entry but not yet committed to the
 * real products table. Lets the user scan and save row by row, and commit
 * everything at the end.
 */
@Entity(tableName = "pending_products", indices = {
        @Index(value = "barcode")
})
public class PendingProduct {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String name;

    public String barcode;

    public double cost = 0;

    public double price = 0;

    public double qty = 1;

    public String categoryId;

    public String unitId;

    public String unitName;

    public long createdAt;
}
