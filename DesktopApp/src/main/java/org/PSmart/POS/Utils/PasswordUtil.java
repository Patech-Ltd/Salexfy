package org.PSmart.POS.Utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Salted PBKDF2 password hashing. Existing plain-text passwords still work at login and are
 * upgraded to a hash the first time the user signs in.
 */
public final class PasswordUtil {

    private static final String PREFIX = "pbkdf2$";
    private static final int ITER = 120_000;

    private PasswordUtil() {}

    public static boolean isHashed(String stored) {
        return stored != null && stored.startsWith(PREFIX);
    }

    public static String hash(String plain) {
        try {
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            byte[] h = derive(plain, salt, ITER);
            return PREFIX + ITER + "$" + Base64.getEncoder().encodeToString(salt) + "$" + Base64.getEncoder().encodeToString(h);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String stored, String plain) {
        if (stored == null || plain == null) return false;
        if (!isHashed(stored)) return stored.equals(plain);     // legacy plain text
        try {
            String[] p = stored.split("\\$");
            int iter = Integer.parseInt(p[1]);
            byte[] salt = Base64.getDecoder().decode(p[2]);
            byte[] expected = Base64.getDecoder().decode(p[3]);
            return MessageDigest.isEqual(expected, derive(plain, salt, iter));
        } catch (Exception e) {
            return false;
        }
    }

    private static byte[] derive(String plain, byte[] salt, int iter) throws Exception {
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(plain.toCharArray(), salt, iter, 256)).getEncoded();
    }
}
