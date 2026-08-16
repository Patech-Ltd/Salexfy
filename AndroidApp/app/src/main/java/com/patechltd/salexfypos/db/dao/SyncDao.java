package com.patechltd.salexfypos.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.patechltd.salexfypos.db.entity.SyncChange;
import com.patechltd.salexfypos.db.entity.SyncLog;

import java.util.List;

@Dao
public interface SyncDao {

    @Insert
    long insertChange(SyncChange change);

    @Insert
    long insertLog(SyncLog log);

    @Query("SELECT * FROM sync_changes WHERE synced = 0 ORDER BY seq ASC LIMIT :limit")
    List<SyncChange> getUnsynced(int limit);

    @Query("SELECT COUNT(*) FROM sync_changes WHERE synced = 0")
    int countUnsynced();

    @Query("SELECT COUNT(*) FROM sync_changes WHERE synced = 0")
    LiveData<Integer> observeUnsyncedCount();

    @Query("UPDATE sync_changes SET synced = 1, updatedAt = :now WHERE seq IN (:seqs)")
    int markSynced(List<Long> seqs, long now);

    @Query("UPDATE sync_changes SET attempts = attempts + 1, lastError = :error, updatedAt = :now WHERE seq = :seq")
    void markError(long seq, String error, long now);

    @Query("DELETE FROM sync_changes WHERE synced = 1 AND seq <= "
            + "(SELECT COALESCE(MAX(seq), 0) - :keep FROM sync_changes)")
    int pruneSynced(int keep);

    @Query("DELETE FROM sync_changes WHERE synced = 0 AND attempts > :maxAttempts")
    int dropFailed(int maxAttempts);

    @Query("SELECT * FROM sync_logs ORDER BY timestamp DESC LIMIT :limit")
    List<SyncLog> getLogs(int limit);

    @Query("DELETE FROM sync_logs WHERE timestamp < :before")
    int pruneLogs(long before);

    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM sync_logs WHERE type = 'PULL' AND status = 'OK'")
    long lastSuccessfulPull();
}
