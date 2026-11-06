package com.patechltd.Salexfy.license;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Public device endpoints (rate limited) + admin endpoints (ROLE_ADMIN, see SecurityConfig). */
@RestController
public class LicenseV2Controller {

    private final LicenseV2Service service;
    private final LicenseSigner signer;

    public LicenseV2Controller(LicenseV2Service service, LicenseSigner signer) {
        this.service = service;
        this.signer = signer;
    }

    // ---- device ----------------------------------------------------------

    @PostMapping("/api/license/v2/activate")
    public LicenseCheckResponse activate(@RequestBody LicenseCheckRequest body, HttpServletRequest req) {
        return service.check(body, ip(req), true);
    }

    @PostMapping("/api/license/v2/validate")
    public LicenseCheckResponse validate(@RequestBody LicenseCheckRequest body, HttpServletRequest req) {
        return service.check(body, ip(req), false);
    }

    @GetMapping("/api/license/v2/public-key")
    public Map<String, String> publicKey() {
        return Map.of("alg", "ES256", "publicKey", signer.publicKeyB64());
    }

    // ---- admin -----------------------------------------------------------

    public record CreateRequest(String shopId, String customerName, String plan,
                                Integer maxDevices, Integer validDays, String notes) { }

    @PostMapping("/api/admin/licenses")
    public Map<String, Object> create(@RequestBody CreateRequest r) {
        int days = r.validDays() == null ? 365 : Math.max(1, Math.min(r.validDays(), 3650));
        long exp = System.currentTimeMillis() + days * 86_400_000L;
        String key = service.create(r.shopId(), r.customerName(), r.plan(),
                r.maxDevices() == null ? 1 : r.maxDevices(), exp, r.notes());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("licenseKey", key);
        out.put("note", "Shown once. Store it securely; only a hash is kept.");
        out.put("expiresAt", exp);
        return out;
    }

    @GetMapping("/api/admin/licenses")
    public List<Map<String, Object>> list() {
        return service.list().stream().map(LicenseV2Controller::view).toList();
    }

    @GetMapping("/api/admin/licenses/{id}")
    public ResponseEntity<Map<String, Object>> get(@PathVariable Long id) {
        return service.get(id).map(l -> {
            Map<String, Object> m = view(l);
            m.put("devices", service.activationsOf(id));
            m.put("audit", service.auditOf(id));
            return ResponseEntity.ok(m);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/admin/licenses/{id}/suspend")
    public ResponseEntity<Void> suspend(@PathVariable Long id) { return ok(service.setStatus(id, License.Status.SUSPENDED)); }

    @PostMapping("/api/admin/licenses/{id}/reactivate")
    public ResponseEntity<Void> reactivate(@PathVariable Long id) { return ok(service.setStatus(id, License.Status.ACTIVE)); }

    @PostMapping("/api/admin/licenses/{id}/revoke")
    public ResponseEntity<Void> revoke(@PathVariable Long id) { return ok(service.setStatus(id, License.Status.REVOKED)); }

    @PostMapping("/api/admin/licenses/{id}/extend")
    public ResponseEntity<Void> extend(@PathVariable Long id, @RequestParam int days) { return ok(service.extend(id, days)); }

    @PostMapping("/api/admin/licenses/{id}/reset-devices")
    public ResponseEntity<Void> reset(@PathVariable Long id) { return ok(service.resetDevices(id)); }

    private static ResponseEntity<Void> ok(boolean found) {
        return found ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    private static Map<String, Object> view(License l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getId());
        m.put("keyHint", "…" + l.getKeyHint());
        m.put("shopId", l.getShopId());
        m.put("customerName", l.getCustomerName());
        m.put("plan", l.getPlan());
        m.put("maxDevices", l.getMaxDevices());
        m.put("rebindsLeft", l.getRebindsLeft());
        m.put("expiresAt", l.getExpiresAt());
        m.put("status", l.getStatus());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }

    /** Behind a proxy set server.forward-headers-strategy=native so this is the real client IP. */
    private static String ip(HttpServletRequest req) {
        return req.getRemoteAddr();
    }
}
