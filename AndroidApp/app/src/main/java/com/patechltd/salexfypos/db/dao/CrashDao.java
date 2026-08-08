package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.patechltd.salexfypos.db.entity.BackupLog;
import com.patechltd.salexfypos.db.entity.CrashLog;

import java.util.List;

@Dao
public interface CrashDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertCrash(CrashLog log);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBackup(BackupLog log);

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<CrashLog>> observeCrashes(int limit);

    @Query("SELECT * FROM crash_logs ORDER BY timestamp DESC LIMIT :limit")
    List<CrashLog> getCrashes(int limit);

    @Query("DELETE FROM crash_logs")
    void clearCrashes();

    @Query("SELECT * FROM backup_logs ORDER BY timestamp DESC LIMIT :limit")
    LiveData<List<BackupLog>> observeBackups(int limit);

    @Query("SELECT * FROM backup_logs ORDER BY timestamp DESC LIMIT :limit")
    List<BackupLog> getBackups(int limit);

    @Query("SELECT COUNT(*) FROM crash_logs")
    int crashCount();
}
