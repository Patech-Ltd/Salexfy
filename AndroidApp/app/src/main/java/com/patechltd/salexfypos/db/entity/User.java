package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "users", indices = {@Index(value = "username", unique = true), @Index(value = "roleId")})
public class User {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public String username;

    public String passwordHash;

    public String salt;

    public String fullName;

    public String roleId;

    public boolean isActive = true;

    public String createdBy;

    public long createdAt;
}
