package com.patechltd.Salexfy.controller;

import com.patechltd.Salexfy.dto.ProvisionRequest;
import com.patechltd.Salexfy.dto.ProvisionResponse;
import com.patechltd.Salexfy.dto.ValidateRequest;
import com.patechltd.Salexfy.dto.ValidateResponse;
import com.patechltd.Salexfy.service.LicenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/license")
public class LicenseController {

    private final LicenseService service;

    public LicenseController(LicenseService service) {
        this.service = service;
    }

    /** The Android app calls this on startup and periodically to validate its license. */
    @PostMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(@RequestBody ValidateRequest request) {
        return ResponseEntity.ok(service.validate(
                request.deviceId(), request.shopId(), request.licenseKey()));
    }

    /** Admin endpoint to provision a new device license. Requires auth. */
    @PostMapping("/provision")
    public ResponseEntity<ProvisionResponse> provision(@RequestBody ProvisionRequest request) {
        long expiry = request.expiresAtMillis() > 0
                ? request.expiresAtMillis()
                : System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000;
        String key = service.provision(request.deviceId(), request.shopId(), expiry);
        return ResponseEntity.ok(new ProvisionResponse(key, request.shopId(), expiry));
    }
}