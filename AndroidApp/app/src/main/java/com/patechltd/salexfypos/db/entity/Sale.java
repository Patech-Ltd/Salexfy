package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sales", indices = {@Index(value = "customerId"), @Index(value = "saleDate"), @Index(value = "cashierId")})
public class Sale {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String saleNo;

    public long saleDate;

    public String cashierId;

    public String cashierName;

    public String customerId;

    public String customerName;

    public double subtotal;

    public double discount;

    public double taxAmount;

    public double total;

    public double paidAmount;

    public double changeAmount;

    public String paymentMethod;

    public String status;

    public double pointsEarned;

    public double pointsRedeemed;

    public String notes;

    public String createdBy;

    public long createdAt;
}
