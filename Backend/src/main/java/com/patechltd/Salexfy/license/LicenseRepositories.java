package com.patechltd.Salexfy.license;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

interface LicenseRepo extends JpaRepository<License, Long> {
    Optional<License> findByKeyHash(String keyHash);
}

interface ActivationRepo extends JpaRepository<LicenseActivation, Long> {
    List<LicenseActivation> findByLicenseId(Long licenseId);
    Optional<LicenseActivation> findByLicenseIdAndDeviceId(Long licenseId, String deviceId);
}

interface AuditRepo extends JpaRepository<LicenseAudit, Long> {
    List<LicenseAudit> findTop200ByLicenseIdOrderByAtDesc(Long licenseId);
}
