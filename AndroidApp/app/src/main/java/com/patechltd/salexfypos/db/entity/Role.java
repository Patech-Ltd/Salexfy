package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "roles")
public class Role {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public String roleName;

    public String authoritiesJson;

    public double commissionPercent;

    public boolean isDefault;

    public long createdAt;
}
