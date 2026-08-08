package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "brands")
public class Brand {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String name;

    public long createdAt;
}
