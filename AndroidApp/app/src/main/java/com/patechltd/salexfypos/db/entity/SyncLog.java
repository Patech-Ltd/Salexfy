package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_logs", indices = {@Index("timestamp")})
public class SyncLog {

    @PrimaryKey
    @NonNull
    public String uid;

    public long timestamp;

    public String type;

    public String status;

    public String message;

    public String details;
}
