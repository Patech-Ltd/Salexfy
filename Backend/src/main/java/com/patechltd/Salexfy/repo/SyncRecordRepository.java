package com.patechltd.Salexfy.repo;

import com.patechltd.Salexfy.entity.SyncRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SyncRecordRepository extends JpaRepository<SyncRecord, Long> {

    Optional<SyncRecord> findByDeviceIdAndDeviceSeq(String deviceId, long deviceSeq);

    @Query("SELECT r FROM SyncRecord r WHERE r.updatedAt > :since AND r.deviceId <> :deviceId "
            + "ORDER BY r.updatedAt ASC, r.id ASC")
    List<SyncRecord> findPullBatch(@Param("since") long since,
                                   @Param("deviceId") String deviceId,
                                   Pageable pageable);
}
