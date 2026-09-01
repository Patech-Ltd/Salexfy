package com.patechltd.Salexfy.dto;

public record ValidateResponse(boolean valid, boolean expired, String message, long expiresAt) {

    public static ValidateResponse ok(long expiresAt) {
        return new ValidateResponse(true, false, "License valid", expiresAt);
    }

    public static ValidateResponse invalid(String message) {
        return new ValidateResponse(false, false, message, 0);
    }

    public static ValidateResponse expired(long expiresAt) {
        return new ValidateResponse(false, true, "License expired", expiresAt);
    }
}