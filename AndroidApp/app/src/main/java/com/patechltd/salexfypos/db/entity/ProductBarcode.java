package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "product_barcodes", indices = {
        @Index(value = "productId"),
        @Index(value = "barcode", unique = true)
})
public class ProductBarcode {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    @ColumnInfo(name = "productId")
    public String productId;

    @ColumnInfo(name = "barcode")
    public String barcode;
}
