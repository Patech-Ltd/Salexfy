package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "customers")
public class Customer {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String name;

    public String phone;

    public String email;

    public String address;

    public String notes;

    public double creditLimit;

    public double loyaltyPoints;

    public double totalSpent;

    public long createdAt;
}
