package com.patechltd.Salexfy.license;

import jakarta.persistence.*;

/** Append-only trail of every license decision (success and failure). */
@Entity
@Table(name = "license_audit", indexes = {@Index(name = "idx_audit_license", columnList = "licenseId")})
public class LicenseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long licenseId;
    private String deviceId;
    private String event;
    @Column(length = 500)
    private String detail;
    private String ip;
    private long at = System.currentTimeMillis();

    public LicenseAudit() { }

    public LicenseAudit(Long licenseId, String deviceId, String event, String detail, String ip) {
        this.licenseId = licenseId;
        this.deviceId = deviceId;
        this.event = event;
        this.detail = detail != null && detail.length() > 500 ? detail.substring(0, 500) : detail;
        this.ip = ip;
    }

    public Long getId() { return id; }
    public Long getLicenseId() { return licenseId; }
    public String getDeviceId() { return deviceId; }
    public String getEvent() { return event; }
    public String getDetail() { return detail; }
    public String getIp() { return ip; }
    public long getAt() { return at; }
}
