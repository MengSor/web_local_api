package org.mengsor.web_local_api.configuration;

import org.mengsor.web_local_api.component.DynamicAuthenticationProvider;
import org.mengsor.web_local_api.component.DynamicAuthorizationManager;
import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.security.apikey.ApiKeyFilter;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;

/**
 * @author mengsor
 * @date 2025/12/21
 * Spring Security configuration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final SettingCacheService settingCacheService;
    private final DynamicAuthenticationProvider authProvider;
    private final DynamicAuthorizationManager authorizationManager;

    public SecurityConfig(@Lazy DynamicAuthenticationProvider authProvider,
                          @Lazy DynamicAuthorizationManager authorizationManager,
                          SettingCacheService settingCacheService) {
        this.authProvider = authProvider;
        this.authorizationManager = authorizationManager;
        this.settingCacheService = settingCacheService;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        SettingCache cache = settingCacheService.load();
        SecurityMode mode = cache.getSecurityMode() != null ? cache.getSecurityMode() : SecurityMode.NONE;

        http
                .securityMatcher(new NegatedRequestMatcher(
                        new AntPathRequestMatcher("/oauth2/**") // token endpoint excluded
                ))
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                .authorizeHttpRequests(auth -> auth
                        // UI always public
                        .requestMatchers(
                                "/", "/home/**", "/setting/**", "/api-config/**",
                                "/css/**", "/js/**", "/images/**", "/webjars/**"
                        ).permitAll()

                        // APIs controlled dynamically
                        .requestMatchers("/api/**", "/v1/**", "/v2/**","/query.api/**")
                        .access(authorizationManager)

                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults()).authenticationProvider(authProvider)
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .addFilterBefore(new ApiKeyFilter(settingCacheService), BasicAuthenticationFilter.class);

//        // Enable BASIC auth if active
//        if (mode == SecurityMode.BASIC) {
//            http.httpBasic(Customizer.withDefaults())
//                    .authenticationProvider(authProvider);
//        }
//
//        // Enable OAuth2 Resource Server if active
//        if (mode == SecurityMode.OAUTH2 || mode == SecurityMode.JWT) {
//            http.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()));
//        }
//
////         For API_KEY, add a custom filter
//        if (mode == SecurityMode.API_KEY) {
//            http.addFilterBefore(new ApiKeyFilter(cache), BasicAuthenticationFilter.class);
//        }

        return http.build();
    }
}
