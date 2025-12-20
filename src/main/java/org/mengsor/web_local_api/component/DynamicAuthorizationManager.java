package org.mengsor.web_local_api.component;

import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.access.intercept.RequestAuthorizationContext;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * @author mengsor
 * @date 2025/12/21
 * Custom authorization manager that checks the security mode stored in the cache.
 */
@Component
public class DynamicAuthorizationManager
        implements AuthorizationManager<RequestAuthorizationContext> {

    private final SettingCacheService cacheService;

    public DynamicAuthorizationManager(SettingCacheService cacheService) {
        this.cacheService = cacheService;
    }

    @Override
    public AuthorizationDecision check(
            Supplier<Authentication> authentication,
            RequestAuthorizationContext context) {

        SettingCache cache = cacheService.loadDecrypted();
        String mode = cache.getSecurityMode().toString();
        // Auth mode NONE → allow everything
        if (mode.equals("NONE")) {
            return new AuthorizationDecision(true);
        }

        // Auth mode BASIC → require authenticated user
        Authentication auth = authentication.get();

        boolean authenticated =
                auth != null &&
                        auth.isAuthenticated() &&
                        !"anonymousUser".equals(auth.getPrincipal());

        return new AuthorizationDecision(authenticated);
    }
}
