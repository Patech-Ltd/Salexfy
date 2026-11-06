package com.patechltd.Salexfy.license;

public record LicenseCheckResponse(boolean ok, String code, String message, String token,
                                   long expiresAt, long leaseUntil, long serverTime) {

    static LicenseCheckResponse deny(String code, String message, long expiresAt) {
        return new LicenseCheckResponse(false, code, message, null, expiresAt, 0, System.currentTimeMillis());
    }
}
