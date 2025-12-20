package org.mengsor.web_local_api.component;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * @author mengsor
 * @date 2025/12/21
 * Custom filter that logs all requests for a specific path (/skyvva.api) and saves them to the memory store.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private final RequestLogService requestLogService;

    public RequestLoggingFilter(RequestLogService requestLogService) {
        this.requestLogService = requestLogService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Only log requests under /skyvva.api
        if (!request.getRequestURI().startsWith("/skyvva.api")) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper wrappedResponse = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();
        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            long duration = System.currentTimeMillis() - startTime;

            String requestBody = new String(wrappedRequest.getContentAsByteArray(), StandardCharsets.UTF_8);
            String responseBody = new String(wrappedResponse.getContentAsByteArray(), StandardCharsets.UTF_8);
            int status = wrappedResponse.getStatus();

            // Store log
            //requestLogService.saveLog(request.getRequestURI(), request.getMethod(), requestBody, responseBody, status, duration);

            // Important: copy body to response so client receives it
            wrappedResponse.copyBodyToResponse();
        }
    }
}
