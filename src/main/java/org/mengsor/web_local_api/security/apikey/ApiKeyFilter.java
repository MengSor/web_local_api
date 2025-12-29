package org.mengsor.web_local_api.security.apikey;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

import static org.springframework.boot.context.properties.source.ConfigurationPropertyName.isValid;


@Component
public class ApiKeyFilter extends OncePerRequestFilter {

    private final SettingCacheService settingCacheService;

    public ApiKeyFilter(SettingCacheService settingCacheService) {
        this.settingCacheService = settingCacheService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain)
            throws IOException, ServletException {

        SecurityMode mode = settingCacheService.load().getSecurityMode();

        if (mode != SecurityMode.API_KEY) {
            chain.doFilter(request, response);
            return;
        }

        String apiKey = request.getHeader("X-API-KEY");
        if (!isValid(apiKey)) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid API Key");
            return;
        }

        chain.doFilter(request, response);
    }
}