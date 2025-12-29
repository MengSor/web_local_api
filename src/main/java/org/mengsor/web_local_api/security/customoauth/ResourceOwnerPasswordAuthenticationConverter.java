package org.mengsor.web_local_api.security.customoauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ResourceOwnerPasswordAuthenticationConverter implements AuthenticationConverter {

    @Override
    public Authentication convert(HttpServletRequest request) {
        String grantType = request.getParameter(OAuth2ParameterNames.GRANT_TYPE);
        if (!"password".equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();
        String username = request.getParameter(OAuth2ParameterNames.USERNAME);
        String password = request.getParameter(OAuth2ParameterNames.PASSWORD);
        String scope = request.getParameter(OAuth2ParameterNames.SCOPE);
        Set<String> scopes = StringUtils.hasText(scope) ?
                new HashSet<>(Arrays.asList(scope.split(" "))) : Collections.emptySet();

        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            OAuth2Error error = new OAuth2Error(OAuth2ErrorCodes.INVALID_REQUEST, "username or password missing", null);
            throw new OAuth2AuthenticationException(error);
        }

        return new ResourceOwnerPasswordAuthenticationToken(clientPrincipal, username, password, scopes);
    }
}

