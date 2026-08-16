package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses", indices = {@Index("expenseDate")})
public class Expense {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String description;

    public String category;

    public double amount;

    public long expenseDate;

    public String createdBy;

    public long createdAt;
}
