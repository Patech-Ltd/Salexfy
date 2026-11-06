package com.patechltd.Salexfy.license;

import jakarta.persistence.*;

/** One device bound to a license, identified by id + hardware model + install date + extras. */
@Entity
@Table(name = "license_activations", indexes = {
        @Index(name = "idx_la_license", columnList = "licenseId"),
        @Index(name = "idx_la_device", columnList = "licenseId,deviceId")
})
public class LicenseActivation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long licenseId;

    @Column(nullable = false)
    private String deviceId;

    private String model;
    private String manufacturer;
    private String appVersion;

    /** Package first-install time (epoch ms) reported by the device. */
    private long installTime;

    /** Hash of stable hardware traits (board, hardware, hashed ANDROID_ID, ...). */
    @Column(length = 64)
    private String hwHash;

    private long firstSeen = System.currentTimeMillis();
    private long lastSeen = System.currentTimeMillis();
    private String lastIp;
    private long validations;
    private boolean revoked;
    private int strikes;

    public Long getId() { return id; }
    public Long getLicenseId() { return licenseId; }
    public void setLicenseId(Long licenseId) { this.licenseId = licenseId; }
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public String getManufacturer() { return manufacturer; }
    public void setManufacturer(String manufacturer) { this.manufacturer = manufacturer; }
    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    public long getInstallTime() { return installTime; }
    public void setInstallTime(long installTime) { this.installTime = installTime; }
    public String getHwHash() { return hwHash; }
    public void setHwHash(String hwHash) { this.hwHash = hwHash; }
    public long getFirstSeen() { return firstSeen; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long lastSeen) { this.lastSeen = lastSeen; }
    public String getLastIp() { return lastIp; }
    public void setLastIp(String lastIp) { this.lastIp = lastIp; }
    public long getValidations() { return validations; }
    public void setValidations(long validations) { this.validations = validations; }
    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }
    public int getStrikes() { return strikes; }
    public void setStrikes(int strikes) { this.strikes = strikes; }
}
