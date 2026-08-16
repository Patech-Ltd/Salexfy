package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sale_payments",
        foreignKeys = @ForeignKey(entity = Sale.class,
                parentColumns = "id", childColumns = "saleId",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("saleId")})
public class SalePayment {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String saleId;

    public String method;

    public double amount;

    public String customerId;

    public String customerName;
}
