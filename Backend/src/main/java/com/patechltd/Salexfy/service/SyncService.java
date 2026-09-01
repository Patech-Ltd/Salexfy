package com.patechltd.Salexfy.service;

import com.patechltd.Salexfy.dto.ChangeDto;
import com.patechltd.Salexfy.dto.PushRequest;
import com.patechltd.Salexfy.entity.SyncRecord;
import com.patechltd.Salexfy.repo.SyncRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class SyncService {

    private static final int PULL_BATCH = 1000;

    private final SyncRecordRepository records;

    public SyncService(SyncRecordRepository records) {
        this.records = records;
    }

    /**
     * Stores a batch of changes idempotently (keyed by device + seq) and
     * returns the seqs that were accepted, so the app can clear them locally.
     */
    @Transactional
    public List<Long> push(PushRequest request) {
        List<Long> acknowledged = new ArrayList<>();
        if (request == null || request.deviceId() == null || request.changes() == null) {
            return acknowledged;
        }
        long now = System.currentTimeMillis();
        for (ChangeDto change : request.changes()) {
            try {
                SyncRecord record = records
                        .findByDeviceIdAndDeviceSeq(request.deviceId(), change.seq())
                        .orElseGet(SyncRecord::new);
                record.setDeviceId(request.deviceId());
                record.setDeviceSeq(change.seq());
                record.setEntityType(change.entityType());
                record.setRecordId(change.recordId());
                record.setOperation(change.operation());
                record.setPayload(change.payload());
                record.setUpdatedAt(now);
                records.save(record);
                acknowledged.add(change.seq());
            } catch (Exception ignored) {
                // Skip malformed rows but keep the rest of the batch.
            }
        }
        return acknowledged;
    }

    /**
     * Returns changes newer than the given timestamp that were pushed by
     * OTHER devices, so a device never receives its own records back.
     */
    public List<ChangeDto> pull(long since, String deviceId) {
        List<ChangeDto> out = new ArrayList<>();
        for (SyncRecord r : records.findPullBatch(since, deviceId == null ? "" : deviceId,
                PageRequest.of(0, PULL_BATCH))) {
            out.add(new ChangeDto(r.getDeviceSeq(), r.getEntityType(), r.getRecordId(),
                    r.getOperation(), r.getPayload(), r.getUpdatedAt()));
        }
        return out;
    }
}
