package org.mengsor.web_local_api.configuration;

import org.mengsor.web_local_api.component.DynamicAuthenticationProvider;
import org.mengsor.web_local_api.component.DynamicAuthorizationManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * @author mengsor
 * @date 2025/12/21
 * Spring Security configuration
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final DynamicAuthenticationProvider authProvider;
    private final DynamicAuthorizationManager authorizationManager;

    public SecurityConfig(DynamicAuthenticationProvider authProvider,
                          DynamicAuthorizationManager authorizationManager) {
        this.authProvider = authProvider;
        this.authorizationManager = authorizationManager;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
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
                        .requestMatchers("/api/**", "/v1/**", "/v2/**","/skyvva.api/**")
                        .access(authorizationManager)

                        .anyRequest().permitAll()
                )

                // Enable Basic Auth support (only triggered when needed)
                .authenticationProvider(authProvider)
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
