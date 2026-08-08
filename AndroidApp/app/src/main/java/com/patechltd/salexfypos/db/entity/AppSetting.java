package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "app_settings")
public class AppSetting {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "key")
    public String key;

    public String value;
}
