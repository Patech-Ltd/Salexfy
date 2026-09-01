package com.patechltd.Salexfy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

/**
 * One synced record from a device. The same shape the Android app pushes:
 * entityType, recordId, operation and the JSON payload.
 */
@Entity
@Table(name = "sync_records", indexes = {
        @Index(name = "idx_sync_device_seq", columnList = "deviceId,deviceSeq", unique = true),
        @Index(name = "idx_sync_updated", columnList = "updatedAt"),
        @Index(name = "idx_sync_shop", columnList = "shopId")
})
public class SyncRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Multi-tenancy: the shop this record belongs to. Prevents cross-shop leakage. */
    @Column(nullable = false)
    private String shopId = "default";

    @Column(nullable = false)
    private String deviceId;

    /** Client-side sequence number (for idempotent re-push). */
    private long deviceSeq;

    @Column(nullable = false)
    private String entityType;

    @Column(nullable = false)
    private String recordId;

    @Column(nullable = false)
    private String operation;

    @Lob
    private String payload;

    private long updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public long getDeviceSeq() {
        return deviceSeq;
    }

    public void setDeviceSeq(long deviceSeq) {
        this.deviceSeq = deviceSeq;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getRecordId() {
        return recordId;
    }

    public void setRecordId(String recordId) {
        this.recordId = recordId;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
