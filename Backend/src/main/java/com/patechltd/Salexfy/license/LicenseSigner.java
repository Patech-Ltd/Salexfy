package com.patechltd.Salexfy.license;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Holds the ECDSA P-256 signing key and the HMAC pepper.
 * Private material comes from env (SALEXFY_LICENSE_PRIVATE_KEY / SALEXFY_LICENSE_PEPPER)
 * or is generated once into a 0600 file under the key directory.
 * The matching PUBLIC key is what gets compiled into the Android app.
 */
@Component
public class LicenseSigner {

    private static final Logger log = LoggerFactory.getLogger(LicenseSigner.class);

    @Value("${salexfy.license.private-key:}")
    private String privateKeyB64;
    @Value("${salexfy.license.pepper:}")
    private String pepperB64;
    @Value("${salexfy.license.key-dir:./data/license-keys}")
    private String keyDir;

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private byte[] pepper;

    @PostConstruct
    void init() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("EC");
        Path dir = Path.of(keyDir);
        Path pkFile = dir.resolve("signing.pk8.b64");
        Path pubFile = dir.resolve("signing.pub.b64");
        Path pepperFile = dir.resolve("pepper.b64");

        String pk = privateKeyB64.isBlank() ? readIfExists(pkFile) : privateKeyB64;
        if (pk == null) {
            KeyPairGenerator g = KeyPairGenerator.getInstance("EC");
            g.initialize(256, new SecureRandom());
            KeyPair kp = g.generateKeyPair();
            pk = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
            writeSecret(pkFile, pk);
            writeSecret(pubFile, Base64.getEncoder().encodeToString(kp.getPublic().getEncoded()));
            log.warn("Generated NEW license signing key in {} - back it up; losing it invalidates all issued tokens", dir);
        }
        privateKey = kf.generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(pk.trim())));
        // derive the public key from the private key (EC: recompute) via stored pub file or env
        String pub = readIfExists(pubFile);
        if (pub == null) {
            throw new IllegalStateException("Public key file missing: " + pubFile
                    + " (needed when SALEXFY_LICENSE_PRIVATE_KEY is supplied via env; also set it from your key backup)");
        }
        publicKey = kf.generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(pub.trim())));

        String pp = pepperB64.isBlank() ? readIfExists(pepperFile) : pepperB64;
        if (pp == null) {
            byte[] b = new byte[32];
            new SecureRandom().nextBytes(b);
            pp = Base64.getEncoder().encodeToString(b);
            writeSecret(pepperFile, pp);
        }
        pepper = Base64.getDecoder().decode(pp.trim());
        log.info("License public key (embed in Android LicenseCrypto.PUBLIC_KEY_B64): {}", publicKeyB64());
    }

    public String publicKeyB64() {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /** Returns base64url(payload) + "." + base64url(DER signature). */
    public String sign(String payloadJson) {
        try {
            byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initSign(privateKey);
            s.update(payload);
            Base64.Encoder enc = Base64.getUrlEncoder().withoutPadding();
            return enc.encodeToString(payload) + "." + enc.encodeToString(s.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Signing failed", e);
        }
    }

    /** Keyed hash of a normalized license key; the raw key is never persisted. */
    public String hashKey(String normalizedKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(normalizedKey.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String readIfExists(Path p) throws Exception {
        return Files.exists(p) ? Files.readString(p).trim() : null;
    }

    private static void writeSecret(Path p, String content) throws Exception {
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
        try {
            Files.setPosixFilePermissions(p, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException ignored) {
            // non-POSIX filesystem
        }
    }
}
