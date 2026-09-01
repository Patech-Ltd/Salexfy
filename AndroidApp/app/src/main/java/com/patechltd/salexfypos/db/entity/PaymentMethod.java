package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "payment_methods")
public class PaymentMethod {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String name;

    public boolean isCredit;

    public boolean isSystem;

    public boolean active = true;

    public int sortOrder;
}