package org.mengsor.web_local_api.component;

import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * @author mengsor
 * @date 2025/12/21
 * Custom authentication provider that checks the username and password against the ones stored in the cache.
 */
@Component
public class DynamicAuthenticationProvider implements AuthenticationProvider {

    private final SettingCacheService cacheService;

    public DynamicAuthenticationProvider(SettingCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public Authentication authenticate(Authentication authentication)
            throws AuthenticationException {

        SettingCache cache = cacheService.loadDecrypted();

        if (cache.getSecurityMode() != null && cache.getSecurityMode().toString().equals("BASIC")) {
            String username = authentication.getName();
            String password = authentication.getCredentials().toString();

            if (username.equals(cache.getUsername()) &&
                    password.equals(cache.getPassword())) {

                return new UsernamePasswordAuthenticationToken(
                        username, password, Collections.singletonList(() -> "ROLE_USER"));
            }
            throw new BadCredentialsException("Invalid username or password");
        }

        // NONE mode → allow any request without credentials
        return new UsernamePasswordAuthenticationToken(
                "anonymous", null, Collections.emptyList());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
