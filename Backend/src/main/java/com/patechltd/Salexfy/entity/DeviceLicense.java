package com.patechltd.Salexfy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Registered device + active license. A license is granted to a shop and
 * bound to a specific device fingerprint. The Android app validates against
 * this table on startup and periodically.
 */
@Entity
@Table(name = "device_licenses", indexes = {
        @Index(name = "idx_dl_device", columnList = "deviceId", unique = true),
        @Index(name = "idx_dl_shop", columnList = "shopId")
})
public class DeviceLicense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Stable external identifier (license key shown to the customer). */
    @Column(nullable = false, unique = true)
    private String licenseKey;

    /** The device fingerprint the license is bound to. */
    @Column(nullable = false)
    private String deviceId;

    /** Multi-tenancy: which shop this device belongs to. */
    @Column(nullable = false)
    private String shopId;

    @Column(nullable = false)
    private boolean isActive = true;

    /** Unix epoch millis when the license expires. */
    @Column(nullable = false)
    private long expiresAt;

    private long createdAt = System.currentTimeMillis();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getLicenseKey() {
        return licenseKey;
    }

    public void setLicenseKey(String licenseKey) {
        this.licenseKey = licenseKey;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getShopId() {
        return shopId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public long getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(long expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}