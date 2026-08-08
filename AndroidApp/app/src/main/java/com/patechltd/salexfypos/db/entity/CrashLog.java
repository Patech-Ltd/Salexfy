package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "crash_logs", indices = {@Index("timestamp")})
public class CrashLog {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    public long timestamp;

    public String threadName;

    public String message;

    public String stackTrace;

    public boolean isFatal;

    public String appVersion;
}
