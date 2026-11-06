package com.patechltd.salexfypos.license;

import android.util.Base64;

import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;

/**
 * Verifies server-signed license tokens (ECDSA P-256 / SHA-256).
 *
 * The server's PUBLIC key is pinned here. Replace {@link #PUBLIC_KEY_B64} with the
 * value the backend prints at start-up ("License public key ...") or returns from
 * GET /api/license/v2/public-key. Only the server holds the private key, so a
 * patched APK or fake server cannot mint a token that verifies.
 */
final class LicenseCrypto {

    /** Base64 X.509 SubjectPublicKeyInfo of the backend signing key. Empty = backend mode unusable. */
    static final String PUBLIC_KEY_B64 = "";

    private LicenseCrypto() {
    }

    static boolean isConfigured() {
        return !PUBLIC_KEY_B64.isEmpty();
    }

    /** @return the verified payload, or null if the token is malformed or the signature is bad. */
    static JSONObject verify(String token) {
        try {
            if (token == null || !isConfigured()) return null;
            int dot = token.indexOf('.');
            if (dot <= 0 || dot != token.lastIndexOf('.')) return null;
            int flags = Base64.URL_SAFE | Base64.NO_PADDING | Base64.NO_WRAP;
            byte[] payload = Base64.decode(token.substring(0, dot), flags);
            byte[] sig = Base64.decode(token.substring(dot + 1), flags);

            PublicKey key = KeyFactory.getInstance("EC").generatePublic(
                    new X509EncodedKeySpec(Base64.decode(PUBLIC_KEY_B64, Base64.DEFAULT)));
            Signature s = Signature.getInstance("SHA256withECDSA");
            s.initVerify(key);
            s.update(payload);
            if (!s.verify(sig)) return null;
            return new JSONObject(new String(payload, StandardCharsets.UTF_8));
        } catch (Exception e) {
            return null;
        }
    }
}
