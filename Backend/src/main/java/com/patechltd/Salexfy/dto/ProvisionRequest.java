package com.patechltd.Salexfy.dto;

public record ProvisionRequest(String deviceId, String shopId, long expiresAtMillis) {
}