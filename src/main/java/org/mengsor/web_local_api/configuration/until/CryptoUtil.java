package org.mengsor.web_local_api.configuration.until;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * @author mengsor
 * @date 2025/12/21
 * Encrypts and decrypts values using AES algorithm.
 */
public class CryptoUtil {

    // EXACTLY 16 chars
    private static final String KEY = "SkyvvaSecretKey!";

    public static String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(), "AES"));
            return Base64.getEncoder()
                    .encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(KEY.getBytes(), "AES"));
            return new String(cipher.doFinal(
                    Base64.getDecoder().decode(encrypted)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
