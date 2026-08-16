package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "debt_payments", indices = {@Index("customerId"), @Index("paymentDate")})
public class DebtPayment {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String customerId;

    public double amount;

    public long paymentDate;

    public String saleId;

    public String method;

    public String notes;

    public String createdBy;

    public long createdAt;
}
