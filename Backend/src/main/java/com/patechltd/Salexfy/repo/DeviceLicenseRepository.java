package com.patechltd.Salexfy.repo;

import com.patechltd.Salexfy.entity.DeviceLicense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceLicenseRepository extends JpaRepository<DeviceLicense, Long> {

    Optional<DeviceLicense> findByLicenseKey(String licenseKey);

    Optional<DeviceLicense> findByDeviceIdAndShopId(String deviceId, String shopId);
}