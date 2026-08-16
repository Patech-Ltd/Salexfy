package com.patechltd.salexfypos.db.entity;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(tableName = "sync_changes", indices = {@Index("synced"), @Index("entityType")})
public class SyncChange {

    @PrimaryKey(autoGenerate = true)
    public long seq;

    @NonNull
    public String entityType;

    @NonNull
    public String recordId;

    @NonNull
    public String operation;

    public String payload;

    public boolean synced = false;

    public int attempts = 0;

    public String lastError;

    public long createdAt;

    public long updatedAt;
}
