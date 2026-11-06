package com.patechltd.Salexfy.license;

import jakarta.persistence.*;

/** A purchasable license. The raw key is never stored - only its keyed hash. */
@Entity
@Table(name = "licenses", indexes = {
        @Index(name = "idx_lic_keyhash", columnList = "keyHash", unique = true),
        @Index(name = "idx_lic_shop", columnList = "shopId")
})
public class License {

    public enum Status { ACTIVE, SUSPENDED, REVOKED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** HMAC-SHA256(pepper, normalizedKey), hex. */
    @Column(nullable = false, unique = true, length = 64)
    private String keyHash;

    /** Last 4 chars of the key, for admin display only. */
    @Column(length = 8)
    private String keyHint;

    @Column(nullable = false)
    private String shopId;

    private String customerName;
    private String plan = "STANDARD";

    /** Max simultaneously bound devices. */
    private int maxDevices = 1;

    /** Extra reinstall re-binds allowed on the same hardware. */
    private int rebindsLeft = 3;

    @Column(nullable = false)
    private long expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.ACTIVE;

    private long createdAt = System.currentTimeMillis();
    private String notes;

    public Long getId() { return id; }
    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }
    public String getKeyHint() { return keyHint; }
    public void setKeyHint(String keyHint) { this.keyHint = keyHint; }
    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getPlan() { return plan; }
    public void setPlan(String plan) { this.plan = plan; }
    public int getMaxDevices() { return maxDevices; }
    public void setMaxDevices(int maxDevices) { this.maxDevices = maxDevices; }
    public int getRebindsLeft() { return rebindsLeft; }
    public void setRebindsLeft(int rebindsLeft) { this.rebindsLeft = rebindsLeft; }
    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }
    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
    public long getCreatedAt() { return createdAt; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
