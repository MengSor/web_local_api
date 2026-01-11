package org.mengsor.web_local_api.security.customoauth;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContext;
import org.springframework.security.oauth2.server.authorization.context.AuthorizationServerContextHolder;
import org.springframework.security.oauth2.server.authorization.token.DefaultOAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.security.Principal;
import java.time.Instant;
import java.util.Collections;
import java.util.Set;

/**
 * @author mengsor
 * @date 2026/01/11
 * This class is specifically designed to support the password grant type and integrates with
 * the configured {@link AuthenticationManager}, {@link OAuth2AuthorizationService},
 * {@link OAuth2TokenGenerator}, and {@link RegisteredClientRepository}.
 */
@Component
@RequiredArgsConstructor
public class ResourceOwnerPasswordAuthenticationProvider implements org.springframework.security.authentication.AuthenticationProvider {

    private final AuthenticationManager authenticationManager;
    private final OAuth2AuthorizationService authorizationService;
    private final OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator;
    private final RegisteredClientRepository registeredClientRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws OAuth2AuthenticationException {
        ResourceOwnerPasswordAuthenticationToken auth = (ResourceOwnerPasswordAuthenticationToken) authentication;

        // 1) Client + user auth
        Authentication clientPrincipal = auth.getClientPrincipal();
        if (!(clientPrincipal instanceof OAuth2ClientAuthenticationToken clientAuth) || !clientAuth.isAuthenticated()) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_CLIENT);
        }

        String clientId = clientAuth.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);
        if (registeredClient == null ||
                registeredClient.getAuthorizationGrantTypes().stream()
                        .noneMatch(gt -> "password".equals(gt.getValue()))) {
            throw new OAuth2AuthenticationException(OAuth2ErrorCodes.UNSUPPORTED_GRANT_TYPE);
        }

        UsernamePasswordAuthenticationToken userPassToken =
                new UsernamePasswordAuthenticationToken(auth.getUsername(), auth.getPassword());
        Authentication userAuth = authenticationManager.authenticate(userPassToken);

        // Determine scopes
        Set<String> authorizedScopes = !CollectionUtils.isEmpty(auth.getScopes())
                ? auth.getScopes()
                : registeredClient.getScopes(); // fallback to client's scopes

        // 2) Common context bits
        AuthorizationServerContext asContext = AuthorizationServerContextHolder.getContext();
        AuthorizationGrantType passwordGrant = new AuthorizationGrantType("password");

        // 3) Generate ACCESS TOKEN (handle Jwt OR AccessToken return types)
        OAuth2TokenContext accessTokenContext = DefaultOAuth2TokenContext.builder()
                .registeredClient(registeredClient)
                .authorizationServerContext(asContext)
                .authorizationGrant(auth)                 // our custom auth token instance
                .authorizationGrantType(passwordGrant)
                .principal(userAuth)
                .tokenType(OAuth2TokenType.ACCESS_TOKEN)
                .authorizedScopes(authorizedScopes)
                .build();

        OAuth2Token generatedAccess = tokenGenerator.generate(accessTokenContext);
        if (generatedAccess == null) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                    "Failed to generate access token", null));
        }

        OAuth2AccessToken accessToken;
        Jwt jwtAccess;
        if (generatedAccess instanceof Jwt jwt) {
            jwtAccess = jwt;
            accessToken = new OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    jwt.getTokenValue(),
                    jwt.getIssuedAt(),
                    jwt.getExpiresAt(),
                    authorizedScopes
            );
        } else {
            jwtAccess = null;
            if (generatedAccess instanceof OAuth2AccessToken at) {
                accessToken = at; // already wrapped
            } else {
                throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.SERVER_ERROR,
                        "Unsupported access token type: " + generatedAccess.getClass().getName(), null));
            }
        }

        // 4) Generate REFRESH TOKEN (if client allows)
        OAuth2RefreshToken refreshToken = null;
        if (registeredClient.getAuthorizationGrantTypes().contains(AuthorizationGrantType.REFRESH_TOKEN)) {
            OAuth2TokenContext refreshTokenContext = DefaultOAuth2TokenContext.builder()
                    .registeredClient(registeredClient)
                    .authorizationServerContext(asContext)
                    .authorizationGrant(auth)
                    .authorizationGrantType(passwordGrant)
                    .principal(userAuth)
                    .tokenType(OAuth2TokenType.REFRESH_TOKEN)
                    .build();

            OAuth2Token generatedRefresh = tokenGenerator.generate(refreshTokenContext);
            if (generatedRefresh instanceof OAuth2RefreshToken rt) {
                refreshToken = rt;
            } else if (generatedRefresh instanceof Jwt jwtRt) {
                // If some custom generator returned a Jwt for refresh (uncommon), wrap it
                Instant issuedAt = jwtRt.getIssuedAt();
                Instant expiresAt = jwtRt.getExpiresAt();
                refreshToken = new OAuth2RefreshToken(jwtRt.getTokenValue(),
                        issuedAt != null ? issuedAt : Instant.now(),
                        expiresAt);
            } // else leave null (no refresh)
        }

        // 5) Persist authorization (tokens in store)
        OAuth2Authorization.Builder authorizationBuilder = OAuth2Authorization.withRegisteredClient(registeredClient)
                .principalName(userAuth.getName())
                .authorizationGrantType(passwordGrant)
                .authorizedScopes(authorizedScopes)
                .attribute(Principal.class.getName(), userAuth);

        authorizationBuilder.token(accessToken, metadata -> {
            if (jwtAccess != null) {
                metadata.put(OAuth2Authorization.Token.CLAIMS_METADATA_NAME, jwtAccess.getClaims());
                metadata.put(OAuth2Authorization.Token.INVALIDATED_METADATA_NAME, false);
            }
        });

        if (refreshToken != null) {
            authorizationBuilder.refreshToken(refreshToken);
        }

        OAuth2Authorization authorization = authorizationBuilder.build();
        authorizationService.save(authorization);

        // 6) Build successful authentication result for token endpoint
        return new OAuth2AccessTokenAuthenticationToken(
                registeredClient, clientPrincipal, accessToken,
                refreshToken, Collections.emptyMap());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return ResourceOwnerPasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
