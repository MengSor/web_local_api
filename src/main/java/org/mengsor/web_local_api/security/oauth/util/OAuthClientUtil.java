package org.mengsor.web_local_api.security.oauth.util;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;

public class OAuthClientUtil {

    private static final int CLIENT_SECRET_LENGTH = 32;

    public static String generateClientId() {
        return UUID.randomUUID().toString();
    }

    public static String generateClientSecret() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] randomBytes = new byte[CLIENT_SECRET_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
}
