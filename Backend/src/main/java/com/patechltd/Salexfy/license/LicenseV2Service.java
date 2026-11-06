package com.patechltd.Salexfy.license;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class LicenseV2Service {

    private static final Pattern SAFE = Pattern.compile("^[\\x20-\\x7E]{1,120}$");
    private static final Pattern B64 = Pattern.compile("^[A-Za-z0-9_\\-+/=]{8,128}$");
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final long DAY = 24L * 60 * 60 * 1000;

    private final LicenseRepo licenses;
    private final ActivationRepo activations;
    private final AuditRepo audit;
    private final LicenseSigner signer;
    private final RateLimiter limiter;
    private final SecureRandom random = new SecureRandom();

    /** How long a signed token keeps the app usable fully offline before it must phone home. */
    @Value("${salexfy.license.lease-days:7}")
    private int leaseDays;

    public LicenseV2Service(LicenseRepo licenses, ActivationRepo activations, AuditRepo audit,
                            LicenseSigner signer, RateLimiter limiter) {
        this.licenses = licenses;
        this.activations = activations;
        this.audit = audit;
        this.signer = signer;
        this.limiter = limiter;
    }

    // ------------------------------------------------------------------ device side

    @Transactional
    public LicenseCheckResponse check(LicenseCheckRequest r, String ip, boolean allowBind) {
        if (!limiter.allow("ip:" + ip, 40, 10 * 60_000L)) {
            return LicenseCheckResponse.deny("RATE_LIMITED", "Too many attempts. Try again later.", 0);
        }
        if (r == null || !valid(r)) {
            return LicenseCheckResponse.deny("BAD_REQUEST", "Malformed request", 0);
        }
        if (!limiter.allow("dev:" + r.deviceId(), 15, 10 * 60_000L)) {
            return LicenseCheckResponse.deny("RATE_LIMITED", "Too many attempts. Try again later.", 0);
        }

        String normalized = normalize(r.licenseKey());
        Optional<License> found = licenses.findByKeyHash(signer.hashKey(normalized));
        if (found.isEmpty()) {
            audit(null, r.deviceId(), "UNKNOWN_KEY", "hint=" + hint(normalized), ip);
            return LicenseCheckResponse.deny("INVALID", "Invalid license key", 0);
        }
        License lic = found.get();
        long now = System.currentTimeMillis();

        if (lic.getStatus() != License.Status.ACTIVE) {
            audit(lic.getId(), r.deviceId(), "DENIED_" + lic.getStatus(), null, ip);
            return LicenseCheckResponse.deny(lic.getStatus().name(),
                    "License is " + lic.getStatus().name().toLowerCase(), lic.getExpiresAt());
        }
        if (lic.getExpiresAt() < now) {
            audit(lic.getId(), r.deviceId(), "EXPIRED", null, ip);
            return LicenseCheckResponse.deny("EXPIRED", "License expired", lic.getExpiresAt());
        }

        LicenseActivation act = activations.findByLicenseIdAndDeviceId(lic.getId(), r.deviceId()).orElse(null);
        if (act != null) {
            if (act.isRevoked()) {
                audit(lic.getId(), r.deviceId(), "DENIED_REVOKED_DEVICE", null, ip);
                return LicenseCheckResponse.deny("DEVICE_REVOKED", "This device was deactivated", lic.getExpiresAt());
            }
            if (!fingerprintMatches(act, r)) {
                act.setStrikes(act.getStrikes() + 1);
                if (act.getStrikes() >= 3) act.setRevoked(true);
                activations.save(act);
                audit(lic.getId(), r.deviceId(), "FINGERPRINT_MISMATCH", "strikes=" + act.getStrikes(), ip);
                return LicenseCheckResponse.deny("DEVICE_MISMATCH",
                        "Device identity does not match this license", lic.getExpiresAt());
            }
        } else {
            if (!allowBind) {
                audit(lic.getId(), r.deviceId(), "NOT_ACTIVATED", null, ip);
                return LicenseCheckResponse.deny("NOT_ACTIVATED", "Device is not activated", lic.getExpiresAt());
            }
            act = bind(lic, r, ip);
            if (act == null) {
                return LicenseCheckResponse.deny("DEVICE_LIMIT",
                        "Device limit reached for this license. Contact support.", lic.getExpiresAt());
            }
        }

        act.setLastSeen(now);
        act.setLastIp(ip);
        act.setValidations(act.getValidations() + 1);
        act.setAppVersion(r.appVersion());
        activations.save(act);
        audit(lic.getId(), r.deviceId(), "OK", null, ip);

        long leaseUntil = Math.min(now + leaseDays * DAY, lic.getExpiresAt());
        String token = signer.sign(payload(lic, act, r, now, leaseUntil));
        return new LicenseCheckResponse(true, "OK", "License valid", token,
                lic.getExpiresAt(), leaseUntil, now);
    }

    private LicenseActivation bind(License lic, LicenseCheckRequest r, String ip) {
        List<LicenseActivation> live = activations.findByLicenseId(lic.getId()).stream()
                .filter(a -> !a.isRevoked()).toList();
        if (live.size() >= lic.getMaxDevices()) {
            // Reinstall on the SAME hardware: swap the old binding, consuming one rebind.
            Optional<LicenseActivation> sameHw = live.stream()
                    .filter(a -> Objects.equals(a.getHwHash(), r.hwHash())
                            && Objects.equals(a.getModel(), r.model())
                            && Objects.equals(a.getManufacturer(), r.manufacturer()))
                    .findFirst();
            if (sameHw.isEmpty() || lic.getRebindsLeft() <= 0) {
                audit(lic.getId(), r.deviceId(), "DEVICE_LIMIT", "live=" + live.size(), ip);
                return null;
            }
            LicenseActivation old = sameHw.get();
            old.setRevoked(true);
            activations.save(old);
            lic.setRebindsLeft(lic.getRebindsLeft() - 1);
            licenses.save(lic);
            audit(lic.getId(), r.deviceId(), "REBOUND", "replaced=" + old.getDeviceId(), ip);
        }
        LicenseActivation a = new LicenseActivation();
        a.setLicenseId(lic.getId());
        a.setDeviceId(r.deviceId());
        a.setModel(r.model());
        a.setManufacturer(r.manufacturer());
        a.setInstallTime(r.installTime());
        a.setHwHash(r.hwHash());
        a.setAppVersion(r.appVersion());
        a.setLastIp(ip);
        audit(lic.getId(), r.deviceId(), "ACTIVATED", r.manufacturer() + " " + r.model(), ip);
        return activations.save(a);
    }

    private static boolean fingerprintMatches(LicenseActivation a, LicenseCheckRequest r) {
        return Objects.equals(a.getModel(), r.model())
                && Objects.equals(a.getManufacturer(), r.manufacturer())
                && Objects.equals(a.getHwHash(), r.hwHash())
                && a.getInstallTime() == r.installTime();
    }

    static String fingerprint(LicenseActivation a) {
        return sha256(a.getDeviceId() + "|" + a.getModel() + "|" + a.getManufacturer()
                + "|" + a.getInstallTime() + "|" + a.getHwHash());
    }

    private String payload(License lic, LicenseActivation act, LicenseCheckRequest r,
                           long now, long leaseUntil) {
        return "{\"v\":1"
                + ",\"lid\":" + lic.getId()
                + ",\"dev\":\"" + esc(act.getDeviceId()) + "\""
                + ",\"fp\":\"" + fingerprint(act) + "\""
                + ",\"shop\":\"" + esc(lic.getShopId()) + "\""
                + ",\"plan\":\"" + esc(lic.getPlan()) + "\""
                + ",\"iat\":" + now
                + ",\"exp\":" + lic.getExpiresAt()
                + ",\"lease\":" + leaseUntil
                + ",\"nonce\":\"" + esc(r.nonce()) + "\"}";
    }

    // ------------------------------------------------------------------ admin side

    /** Creates a license and returns the RAW key - the only time it is ever visible. */
    @Transactional
    public String create(String shopId, String customer, String plan, int maxDevices,
                         long expiresAt, String notes) {
        String key = generateKey();
        License l = new License();
        l.setKeyHash(signer.hashKey(normalize(key)));
        l.setKeyHint(normalize(key).substring(normalize(key).length() - 4));
        l.setShopId(shopId == null || shopId.isBlank() ? "default" : shopId.trim());
        l.setCustomerName(customer);
        l.setPlan(plan == null || plan.isBlank() ? "STANDARD" : plan.trim());
        l.setMaxDevices(Math.max(1, Math.min(maxDevices, 50)));
        l.setExpiresAt(expiresAt);
        l.setNotes(notes);
        licenses.save(l);
        audit(l.getId(), null, "CREATED", "shop=" + l.getShopId(), "admin");
        return key;
    }

    public List<License> list() { return licenses.findAll(); }

    public Optional<License> get(Long id) { return licenses.findById(id); }

    public List<LicenseActivation> activationsOf(Long id) { return activations.findByLicenseId(id); }

    public List<LicenseAudit> auditOf(Long id) { return audit.findTop200ByLicenseIdOrderByAtDesc(id); }

    @Transactional
    public boolean setStatus(Long id, License.Status status) {
        return licenses.findById(id).map(l -> {
            l.setStatus(status);
            licenses.save(l);
            audit(id, null, "STATUS_" + status, null, "admin");
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean extend(Long id, int days) {
        return licenses.findById(id).map(l -> {
            long base = Math.max(l.getExpiresAt(), System.currentTimeMillis());
            l.setExpiresAt(base + Math.max(1, Math.min(days, 3650)) * DAY);
            licenses.save(l);
            audit(id, null, "EXTENDED", "days=" + days, "admin");
            return true;
        }).orElse(false);
    }

    @Transactional
    public boolean resetDevices(Long id) {
        return licenses.findById(id).map(l -> {
            for (LicenseActivation a : activations.findByLicenseId(id)) {
                a.setRevoked(true);
                activations.save(a);
            }
            audit(id, null, "DEVICES_RESET", null, "admin");
            return true;
        }).orElse(false);
    }

    @Scheduled(fixedDelay = 600_000)
    void sweepLimiter() { limiter.sweep(10 * 60_000L); }

    // ------------------------------------------------------------------ helpers

    private void audit(Long licenseId, String device, String event, String detail, String ip) {
        audit.save(new LicenseAudit(licenseId, device, event, detail, ip));
    }

    private static boolean valid(LicenseCheckRequest r) {
        return r.licenseKey() != null && r.licenseKey().length() <= 64
                && r.deviceId() != null && SAFE.matcher(r.deviceId()).matches()
                && r.model() != null && SAFE.matcher(r.model()).matches()
                && r.manufacturer() != null && SAFE.matcher(r.manufacturer()).matches()
                && r.hwHash() != null && B64.matcher(r.hwHash()).matches()
                && r.installTime() > 0
                && r.nonce() != null && B64.matcher(r.nonce()).matches()
                && (r.appVersion() == null || r.appVersion().length() <= 32);
    }

    static String normalize(String key) {
        return key == null ? "" : key.replaceAll("[\\s-]", "").toUpperCase();
    }

    private static String hint(String normalized) {
        return normalized.length() < 4 ? "?" : "..." + normalized.substring(normalized.length() - 4);
    }

    private String generateKey() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 25; i++) {
            if (i > 0 && i % 5 == 0) sb.append('-');
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    private static String sha256(String s) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') b.append('\\');
            if (c >= 0x20) b.append(c);
        }
        return b.toString();
    }
}
