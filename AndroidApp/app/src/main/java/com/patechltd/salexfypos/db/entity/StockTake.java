package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "stock_takes", indices = {@Index("stockTakeDate")})
public class StockTake {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String name;

    public long stockTakeDate;

    public String status;

    public String notes;

    public String createdBy;

    public long createdAt;
}
