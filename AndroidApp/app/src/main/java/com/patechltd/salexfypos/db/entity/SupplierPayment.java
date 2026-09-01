package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "supplier_payments",
        indices = {@Index(value = "purchaseId"), @Index(value = "paymentDate")})
public class SupplierPayment {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String purchaseId;

    public double amount;

    public long paymentDate;

    public String method;

    public String notes;

    public String createdBy;

    public long createdAt;
}