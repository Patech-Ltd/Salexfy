package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sale_items",
        foreignKeys = @ForeignKey(entity = Sale.class,
                parentColumns = "id", childColumns = "saleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("saleId"), @Index("productId")})
public class SaleItem {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String saleId;

    public String productId;

    public String productName;

    public String barcode;

    public double qty;

    public double stockQty;

    public String unitLabel;

    public boolean isWholesale;

    public double unitPrice;

    public double costPrice;

    public double lineTotal;
}
