package org.mengsor.web_local_api.configuration.until;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * @author mengsor
 * @date 2025/12/21
 * Encrypts and decrypts values using AES algorithm.
 */
public class CryptoUtil {

    // EXACTLY 16 chars
    private static final String KEY = "SkyvvaSecretKey!";

    private static final String AES = "AES";

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), AES));
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts value.
     * If value is NOT encrypted, returns it as-is.
     */
    public static String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        try {
            // 1️⃣ Validate Base64 first
            if (!isBase64(value)) {
                return value; // plain text
            }

            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), AES));

            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(value));
            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            // 2️⃣ Any crypto error → treat as plain text
            return value;
        }
    }

    private static boolean isBase64(String value) {
        try {
            // try URL-safe first
            Base64.getUrlDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            try {
                Base64.getDecoder().decode(value);
                return true;
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }
    }

    public static String encryptV(String value) {
        try {
            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), AES)
            );

            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));

            // ✅ URL SAFE (NO + / =)
            return Base64.getUrlEncoder()
                    .withoutPadding()
                    .encodeToString(encrypted);

        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    /**
     * Decrypts value.
     * If value is NOT encrypted, returns it as-is.
     */
    public static String decryptV(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        try {
            // 1️⃣ Validate Base64 (URL-safe + normal)
            if (!isBase64(value)) {
                return value;
            }

            Cipher cipher = Cipher.getInstance(AES);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(StandardCharsets.UTF_8), AES)
            );

            byte[] decoded = Base64.getUrlDecoder().decode(value);
            byte[] decrypted = cipher.doFinal(decoded);

            return new String(decrypted, StandardCharsets.UTF_8);

        } catch (Exception e) {
            // 2️⃣ Any crypto error → treat as plain text
            return value;
        }
    }


}
