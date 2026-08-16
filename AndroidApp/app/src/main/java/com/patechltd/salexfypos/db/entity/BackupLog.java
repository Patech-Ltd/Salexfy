package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "backup_logs")
public class BackupLog {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String uid;

    public long timestamp;

    public String type;

    public String status;

    public String filePath;

    public String message;
}
