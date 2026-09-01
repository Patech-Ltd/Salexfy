package com.patechltd.Salexfy.service;

import com.patechltd.Salexfy.dto.ValidateResponse;
import com.patechltd.Salexfy.entity.DeviceLicense;
import com.patechltd.Salexfy.repo.DeviceLicenseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Optional;

@Service
public class LicenseService {

    private final DeviceLicenseRepository licenses;
    private final SecureRandom random = new SecureRandom();

    public LicenseService(DeviceLicenseRepository licenses) {
        this.licenses = licenses;
    }

    /**
     * Validates that the given device + license key is active, not expired, and
     * belongs to the declared shop. This is the core license gate.
     */
    @Transactional(readOnly = true)
    public ValidateResponse validate(String deviceId, String shopId, String licenseKey) {
        if (deviceId == null || deviceId.isBlank() || licenseKey == null || licenseKey.isBlank()) {
            return ValidateResponse.invalid("Missing device or license key");
        }
        Optional<DeviceLicense> found = licenses.findByLicenseKey(licenseKey.trim());
        if (found.isEmpty()) {
            return ValidateResponse.invalid("Unknown license key");
        }
        DeviceLicense lic = found.get();
        if (!lic.isActive()) {
            return ValidateResponse.invalid("License has been deactivated");
        }
        if (shopId != null && !shopId.isBlank() && !shopId.equals(lic.getShopId())) {
            return ValidateResponse.invalid("License does not belong to this shop");
        }
        if (!lic.getDeviceId().equals(deviceId.trim())) {
            return ValidateResponse.invalid("License is bound to another device");
        }
        long now = System.currentTimeMillis();
        if (lic.getExpiresAt() < now) {
            return ValidateResponse.expired(lic.getExpiresAt());
        }
        return ValidateResponse.ok(lic.getExpiresAt());
    }

    /**
     * Registers a new device+license for a shop. Returns a human-friendly
     * license key. Normally called by an admin/CSV import; also exposed as a
     * manual endpoint for provisioning.
     */
    @Transactional
    public String provision(String deviceId, String shopId, long expiresAtMillis) {
        DeviceLicense lic = new DeviceLicense();
        lic.setDeviceId(deviceId);
        lic.setShopId(shopId == null || shopId.isBlank() ? "default" : shopId);
        lic.setExpiresAt(expiresAtMillis);
        lic.setLicenseKey(generateKey());
        lic.setActive(true);
        licenses.save(lic);
        return lic.getLicenseKey();
    }

    private String generateKey() {
        StringBuilder sb = new StringBuilder();
        String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        for (int i = 0; i < 20; i++) {
            if (i > 0 && i % 5 == 0) sb.append('-');
            sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return sb.toString();
    }
}