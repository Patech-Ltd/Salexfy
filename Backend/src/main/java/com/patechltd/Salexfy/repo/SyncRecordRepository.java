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

    /**
     * Returns changes newer than the given timestamp from OTHER devices in the
     * SAME shop, so a device never receives its own records back and never
     * receives another shop's data.
     */
    @Query("SELECT r FROM SyncRecord r WHERE r.shopId = :shopId AND r.updatedAt > :since "
            + "AND r.deviceId <> :deviceId ORDER BY r.updatedAt ASC, r.id ASC")
    List<SyncRecord> findPullBatch(@Param("shopId") String shopId,
                                   @Param("since") long since,
                                   @Param("deviceId") String deviceId,
                                   Pageable pageable);
}