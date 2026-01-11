package org.mengsor.web_local_api.security.configsecurity;


import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.mengsor.web_local_api.security.customoauth.CustomOAuth2AuthenticationConverter;
import org.mengsor.web_local_api.security.customoauth.ResourceOwnerPasswordAuthenticationProvider;
import org.mengsor.web_local_api.security.oauth.util.Jwks;
import org.mengsor.web_local_api.security.services.CustomRegisteredClientRepository;
import org.mengsor.web_local_api.security.services.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.InMemoryOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.*;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;

/**
 * @author mengsor
 * @date 2026/01/11
 * It leverages Spring Security's libraries and offers custom configurations for handling
 * security-related concerns such as JWT token generation, OAuth2 token flows, and custom grant types.
 * The class also integrates application-specific service components such as
 * CustomUserDetailsService and CustomRegisteredClientRepository.
 *
 * Key responsibilities and components:
 * - {@link AuthenticationManager}: Configures authentication providers and manages authentication processes.
 * - {@link JWKSource}: Provides a JSON Web Key source used for signing and validating JWT tokens.
 * - {@link SecurityFilterChain}: Defines the security filter chain for the OAuth2 Authorization Server.
 * - {@link OAuth2AuthorizationService}: Manages the persistence and retrieval of OAuth2 authorization data.
 * - {@link OAuth2TokenGenerator}: Generates OAuth2 tokens such as access tokens and refresh tokens.
 */
@Configuration
@RequiredArgsConstructor
public class AuthorizationServerConfig {

    private final @Lazy CustomRegisteredClientRepository registeredClientRepository;
    private final CustomUserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider daoProvider = new DaoAuthenticationProvider();
        daoProvider.setUserDetailsService(userDetailsService);
        daoProvider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(daoProvider);
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        RSAKey rsaKey = Jwks.generateRsa();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return (jwkSelector, securityContext) -> jwkSelector.select(jwkSet);
    }

    @Bean
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);

        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .tokenEndpoint(tokenEndpoint -> tokenEndpoint
                        .accessTokenRequestConverter(new CustomOAuth2AuthenticationConverter())
                        .authenticationProvider(new ResourceOwnerPasswordAuthenticationProvider(
                                authenticationManager(),
                                authorizationService(),
                                tokenGenerator((jwkSelector, securityContext) -> jwkSelector.select(new JWKSet())),
                                registeredClientRepository
                        ))
                );

        return http.build();
    }

    @Bean
    public OAuth2AuthorizationService authorizationService() {
        return new InMemoryOAuth2AuthorizationService();
    }

    @Bean
    public OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator(JWKSource<SecurityContext> jwkSource) {
        // JWT encoder/generator (for signed JWT access tokens)
        JwtEncoder jwtEncoder = new NimbusJwtEncoder(jwkSource);
        JwtGenerator jwtGenerator = new JwtGenerator(jwtEncoder);

        // Also include standard generators so Delegating can return either Jwt or OAuth2AccessToken/RefreshToken
        OAuth2AccessTokenGenerator accessTokenGenerator = new OAuth2AccessTokenGenerator();
        OAuth2RefreshTokenGenerator refreshTokenGenerator = new OAuth2RefreshTokenGenerator();

        return new DelegatingOAuth2TokenGenerator(jwtGenerator, accessTokenGenerator, refreshTokenGenerator);
    }

}