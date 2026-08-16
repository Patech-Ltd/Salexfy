package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "purchases", indices = {@Index(value = "supplierId"), @Index(value = "purchaseDate")})
public class Purchase {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String invoiceNo;

    public String supplierId;

    public long purchaseDate;

    public double subtotal;

    public double discount;

    public double total;

    public double paidAmount;

    public String status;

    public String notes;

    public String createdBy;

    public long createdAt;
}
