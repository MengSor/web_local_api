package org.mengsor.web_local_api.security.customoauth;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.Collections;
import java.util.Set;

/**
 * @author mengsor
 * @date 2026/01/11
 * Represents an authentication token for the Resource Owner Password Credentials Grant in OAuth 2.0.
 * This is used to encapsulate the username, password, client principal, and requested scopes
 * associated with this authentication mechanism.
 *
 * This class extends the {@link AbstractAuthenticationToken} and provides specific attributes
 * required for the resource owner password grant flow, including user credentials and associated client details.
 *
 * The authentication token is initially unauthenticated and must be set as authenticated after successful authentication.
 */
public class ResourceOwnerPasswordAuthenticationToken extends AbstractAuthenticationToken {

    private final String username;
    private final String password;
    private final Authentication clientPrincipal;
    private final Set<String> scopes;

    public ResourceOwnerPasswordAuthenticationToken(Authentication clientPrincipal,
                                                    String username,
                                                    String password,
                                                    Set<String> scopes) {
        super(null);
        this.clientPrincipal = clientPrincipal;
        this.username = username;
        this.password = password;
        this.scopes = (scopes != null ? scopes : Collections.emptySet());
        setAuthenticated(false);
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public Authentication getClientPrincipal() {
        return clientPrincipal;
    }

    public Set<String> getScopes() {
        return scopes;
    }

    @Override
    public Object getCredentials() {
        return password;
    }

    @Override
    public Object getPrincipal() {
        return username;
    }
}
