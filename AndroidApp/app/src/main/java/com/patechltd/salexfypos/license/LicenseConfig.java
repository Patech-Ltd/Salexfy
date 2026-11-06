package com.patechltd.salexfypos.license;

/**
 * Build-time licensing switches. Not exposed anywhere in the app UI.
 * Change these values BEFORE building the APK.
 *
 *   Mode.LEGACY      built-in license gate (install-date VersionGate): the original behaviour.
 *   Mode.BACKEND     secure Spring Boot backend licensing (signed tokens). Also set
 *                    LicenseCrypto.PUBLIC_KEY_B64 to the server's public key.
 *   Mode.GOOGLE_PLAY the shop must have purchased the product through Google Play Billing.
 */
public final class LicenseConfig {

    public enum Mode { LEGACY, BACKEND, GOOGLE_PLAY }

    public static final Mode MODE = Mode.LEGACY;

    // ---- Google Play settings (used only when MODE == GOOGLE_PLAY) ----------------------
    /** Product / subscription id created in Play Console > Monetize. */
    public static final String PLAY_PRODUCT_ID = "salexfy_pos_license";
    /** true = auto-renewing subscription, false = one-time in-app product. */
    public static final boolean PLAY_IS_SUBSCRIPTION = true;
    /** Days the app keeps working offline after the last successful Play check. */
    public static final int PLAY_OFFLINE_GRACE_DAYS = 14;

    private LicenseConfig() {
    }
}
