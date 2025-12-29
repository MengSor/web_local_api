package org.mengsor.web_local_api.security.services;

import lombok.RequiredArgsConstructor;
import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Service
@Primary
@RequiredArgsConstructor
public class CustomRegisteredClientRepository
        implements RegisteredClientRepository {

    private final SettingCacheService settingCacheService;
    private final PasswordEncoder passwordEncoder;

    private static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");
    private static final AuthorizationGrantType CLIENT_CREDENTIALS = new AuthorizationGrantType("client_credentials");
    private static final AuthorizationGrantType REFRESH_TOKEN = new AuthorizationGrantType("refresh_token");
    private static final AuthorizationGrantType ACCESS_TOKEN = new AuthorizationGrantType("access_token");

    @Override
    public void save(RegisteredClient registeredClient) {
        // no-op
    }

    @Override
    public RegisteredClient findById(String id) {
        return loadFromCache();
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        SettingCache cache = settingCacheService.load();
        if (!SecurityMode.OAUTH2.equals(cache.getSecurityMode())) return null;
        return clientId.equals(cache.getClientId()) ? loadFromCache() : null;
    }

    private RegisteredClient loadFromCache() {
        SettingCache cache = settingCacheService.load();
        if (!SecurityMode.OAUTH2.equals(cache.getSecurityMode())) return null;

        return RegisteredClient.withId(cache.getClientId()) // ✅ STABLE ID
                .clientId(cache.getClientId())
                .clientSecret(passwordEncoder.encode(cache.getClientSecret())) // ✅ STABLE SECRET
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)

                // ✅ GRANTS
                .authorizationGrantType(PASSWORD)
                .authorizationGrantType(CLIENT_CREDENTIALS)
                .authorizationGrantType(REFRESH_TOKEN)

                .scope("read")
                .scope("write")

                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(
                                Duration.of(cache.getTokenDuration(),
                                        ChronoUnit.valueOf(cache.getTokenUnit().name())))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build())
                .build();
    }
}

