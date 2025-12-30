package org.mengsor.web_local_api.security.customoauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientCredentialsAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2RefreshTokenAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

public class CustomOAuth2AuthenticationConverter implements AuthenticationConverter {

    private static final AuthorizationGrantType PASSWORD = new AuthorizationGrantType("password");

    @Override
    public Authentication convert(HttpServletRequest request) {

        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!StringUtils.hasText(grantType)) {
            return null;
        }

        Authentication clientPrincipal =
                SecurityContextHolder.getContext().getAuthentication();

        if (PASSWORD.getValue().equals(grantType)) {
            return handlePasswordGrant(request, clientPrincipal);
        }

        if (AuthorizationGrantType.CLIENT_CREDENTIALS.getValue().equals(grantType)) {
            return handleClientCredentialsGrant(request, clientPrincipal);
        }

        if (AuthorizationGrantType.REFRESH_TOKEN.getValue().equals(grantType)) {
            return handleRefreshTokenGrant(request, clientPrincipal);
        }

        return null;
    }

    /* ================= PASSWORD ================= */

    private Authentication handlePasswordGrant(
            HttpServletRequest request,
            Authentication clientPrincipal) {

        String username = request.getParameter(OAuth2ParameterNames.USERNAME);
        String password = request.getParameter(OAuth2ParameterNames.PASSWORD);

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            throw invalidRequest("username or password missing");
        }

        return new ResourceOwnerPasswordAuthenticationToken(
                clientPrincipal,
                username,
                password,
                parseScopes(request)
        );
    }

    /* ================= CLIENT CREDENTIALS ================= */

    private Authentication handleClientCredentialsGrant(
            HttpServletRequest request,
            Authentication clientPrincipal) {

        Set<String> scopes = parseScopes(request);

        Map<String, Object> additionalParameters =
                request.getParameterMap().entrySet().stream()
                        .filter(e -> !OAuth2ParameterNames.GRANT_TYPE.equals(e.getKey()))
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> e.getValue()[0]
                        ));

        return new OAuth2ClientCredentialsAuthenticationToken(
                clientPrincipal,
                scopes,
                additionalParameters
        );
    }


    /* ================= REFRESH TOKEN ================= */

    private Authentication handleRefreshTokenGrant(
            HttpServletRequest request,
            Authentication clientPrincipal) {

        String refreshToken =
                request.getParameter(OAuth2ParameterNames.REFRESH_TOKEN);

        if (!StringUtils.hasText(refreshToken)) {
            throw invalidRequest("refresh_token missing");
        }

        Set<String> scopes = parseScopes(request);

        return new OAuth2RefreshTokenAuthenticationToken(
                refreshToken,
                clientPrincipal,
                scopes,
                Collections.emptyMap()
        );
    }


    /* ================= HELPERS ================= */

    private Set<String> parseScopes(HttpServletRequest request) {
        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
        return StringUtils.hasText(scope)
                ? new HashSet<>(Arrays.asList(scope.split(" ")))
                : Collections.emptySet();
    }

    private OAuth2AuthenticationException invalidRequest(String message) {
        return new OAuth2AuthenticationException(
                new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, message, null)
        );
    }
}

