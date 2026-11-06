package com.patechltd.Salexfy.license;

/**
 * Device-reported facts. {@code nonce} is a client random value echoed inside the
 * signed token so a recorded response cannot be replayed by a fake server.
 */
public record LicenseCheckRequest(String licenseKey, String deviceId, String model,
                                  String manufacturer, long installTime, String hwHash,
                                  String appVersion, String nonce) {
}
