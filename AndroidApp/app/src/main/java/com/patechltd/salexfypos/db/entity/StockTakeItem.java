package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "stock_take_items",
        foreignKeys = @ForeignKey(entity = StockTake.class,
                parentColumns = "id", childColumns = "stockTakeId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("stockTakeId"), @Index("productId")})
public class StockTakeItem {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String stockTakeId;

    public String productId;

    public double systemQty;

    public double countedQty;

    public double diffQty;
}
