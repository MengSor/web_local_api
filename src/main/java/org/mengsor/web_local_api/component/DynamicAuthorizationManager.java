package org.mengsor.web_local_api.component;

import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.AuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
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
        SecurityMode mode = cache.getSecurityMode() != null ? cache.getSecurityMode() : SecurityMode.NONE;

        Authentication auth = authentication.get();

        boolean allowed;

        switch (mode) {
            case NONE:
                allowed = true; // public access
                break;

            case BASIC:
                allowed = auth != null &&
                        auth.isAuthenticated() &&
                        auth instanceof UsernamePasswordAuthenticationToken &&
                        !"anonymousUser".equals(auth.getPrincipal());
                break;

            case OAUTH2:
                allowed = auth != null &&
                        auth.isAuthenticated() &&
                        auth instanceof JwtAuthenticationToken; // OAuth2 JWT
                break;

            case JWT:
                allowed = auth != null &&
                        auth.isAuthenticated() &&
                        auth instanceof JwtAuthenticationToken; // JWT bearer
                break;

            case API_KEY:
                // Simple check: role "ROLE_API" must be present
                allowed = auth != null &&
                        auth.isAuthenticated() &&
                        auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("ROLE_API"));
                break;

            default:
                allowed = false;
        }

        return new AuthorizationDecision(allowed);
    }
}