package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "stock_movements", indices = {
        @Index("productId"),
        @Index("createdAt"),
        @Index(value = {"productId", "createdAt"})
})
public class StockMovement {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String productId;

    public String movementType;

    public double qty;

    public double stockBefore;

    public double stockAfter;

    public String refId;

    public String unitLabel;

    public String note;

    public String createdBy;

    public long createdAt;
}
