package org.mengsor.web_local_api.services.serviceImpl;


import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.component.RequestMismatchReporter;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.mengsor.web_local_api.services.ApiConfigService;
import org.mengsor.web_local_api.services.DynamicApiService;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class DynamicApiServiceImpl implements DynamicApiService {

    private final ApiConfigService apiConfigService;
    private final RequestLogService requestLogService;
    private final RequestMismatchReporter reporter;

    public DynamicApiServiceImpl(ApiConfigService apiConfigService,
                                 RequestLogService requestLogService,
                                 RequestMismatchReporter reporter) {
        this.apiConfigService = apiConfigService;
        this.requestLogService = requestLogService;
        this.reporter = reporter;
    }

    @Override
    public ApiResponse handleRequest(HttpServletRequest request, String requestBody) {

        log.info("Received request: {} {}", request.getMethod(), request.getRequestURI());
        String apiPath = request.getRequestURI().replaceFirst(".*/skyvva.api/", "");
        String method = request.getMethod();

        ApiConfig config = apiConfigService.findAll().stream()
                .filter(api -> apiPath.equalsIgnoreCase(api.getUrl()) &&
                        method.equalsIgnoreCase(api.getMethod()))
                .findFirst()
                .orElse(null);

        if (config == null) {
            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    null,
                    "API not configured",
                    "No matching API found for " + method + " " + apiPath,
                    HttpStatus.NOT_FOUND.value()
            );
            log.error("API not configured: {} {}", method, apiPath);
            return new ApiResponse(false, "API not configured: " + method + " " + apiPath,
                    null, HttpStatus.NOT_FOUND.value());
        }

        // Validate format
        if (!validateFormatSafe(requestBody, config.getRequestBody())) {
            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    config,
                    "Request body format invalid",
                    null,
                    HttpStatus.BAD_REQUEST.value()
            );
            log.error("Request body format invalid: {}", requestBody);
            return new ApiResponse(false, "Request body format invalid", null,
                    HttpStatus.BAD_REQUEST.value());
        }

        // Validate content
        List<String> headerDiffs = new ArrayList<>();
        if (!config.getHeaders().isEmpty() && config.getHeaders().size() > 0) {
            headerDiffs  = reporter.compareHeaders(request, config.getHeaders());
        }
        if ((hasTemplate(config.getRequestBody()) && !reporter.bodyEquals(config.getRequestBody(), requestBody))
             || (config.getHeaders()!=null && headerDiffs.size() >0 && !headerDiffs.isEmpty())) {
            String diffReport = reporter.buildNonMatchReport(
                    request,
                    config,
                    requestBody,
                    headerDiffs
            );

            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    config,
                    "Request does not match template",
                    diffReport,
                    HttpStatus.BAD_REQUEST.value()
            );
            log.error("Request does not match template: {}", requestBody);
            return new ApiResponse(false, "Request does not match template", null,
                    HttpStatus.BAD_REQUEST.value());
        }

        // Prepare response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(detectMediaType(config.getResponseBody()));

        // Log success
        requestLogService.logMatched(
                request,
                requestBody,
                config,
                config.getResponseBody(),
                config.getStatusCode()
        );

        log.info("Received request successfully");
        return new ApiResponse(true, "Success", config.getResponseBody(), config.getStatusCode());
    }

    // --- Helper methods ---
    private boolean requiresBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }

    private boolean hasTemplate(String template) {
        log.debug("Checking if template is present: {}", template);
        return template != null && !template.isBlank();
    }

    private boolean validateFormatSafe(String requestBody, String template) {
        log.debug("Validating format of request body: {} against template: {}", requestBody, template);
        try {
            reporter.validateFormat(requestBody, template);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private MediaType detectMediaType(String body) {
        if (body == null) return MediaType.TEXT_PLAIN;
        String t = body.trim();
        if (t.startsWith("{")) return MediaType.APPLICATION_JSON;
        if (t.startsWith("<")) return MediaType.APPLICATION_XML;
        return MediaType.TEXT_PLAIN;
    }
}
