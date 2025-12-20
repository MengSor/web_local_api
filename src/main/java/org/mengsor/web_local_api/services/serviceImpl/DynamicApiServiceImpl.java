package org.mengsor.web_local_api.services.serviceImpl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.mengsor.web_local_api.services.ApiConfigService;
import org.mengsor.web_local_api.services.DynamicApiService;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

@Service
public class DynamicApiServiceImpl implements DynamicApiService {

    private final ApiConfigService apiConfigService;
    private final RequestLogService requestLogService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicApiServiceImpl(ApiConfigService apiConfigService,
                                 RequestLogService requestLogService) {
        this.apiConfigService = apiConfigService;
        this.requestLogService = requestLogService;
    }

    @Override
    public ApiResponse handleRequest(HttpServletRequest request, String requestBody) {
        String apiPath = request.getRequestURI().replaceFirst(".*/skyvva.api/", "");
        String method = request.getMethod();

        ApiConfig config = apiConfigService.findAll().stream()
                .filter(api -> apiPath.equalsIgnoreCase(api.getUrl()) &&
                        method.equalsIgnoreCase(api.getMethod()))
                .findFirst()
                .orElse(null);

        if (config == null) {
            requestLogService.logFailed(request, requestBody, "API not configured");
            return new ApiResponse(false, "API not configured: " + method + " " + apiPath,
                    null, HttpStatus.NOT_FOUND.value());
        }

        // Check request body requirement
        if (requiresBody(method) && hasTemplate(config.getRequestBody())) {
            if (requestBody == null || requestBody.isBlank()) {
                requestLogService.logFailed(request, requestBody, "Request body is required");
                return new ApiResponse(false, "Request body is required", null,
                        HttpStatus.BAD_REQUEST.value());
            }
        }

        // Validate format
        if (!validateFormatSafe(requestBody, config.getRequestBody())) {
            requestLogService.logFailed(request, requestBody, "Request body format invalid");
            return new ApiResponse(false, "Request body format invalid", null,
                    HttpStatus.BAD_REQUEST.value());
        }

        // Validate content
        if (hasTemplate(config.getRequestBody()) &&
                !bodyEquals(config.getRequestBody(), requestBody)) {
            requestLogService.logFailed(request, requestBody, "Request body does not match template");
            return new ApiResponse(false, "Request body does not match template", null,
                    HttpStatus.BAD_REQUEST.value());
        }

        // Prepare response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(detectMediaType(config.getResponseBody()));

        // Log success
        requestLogService.logMatched(request, requestBody, config);

        return new ApiResponse(true, "Success", config.getResponseBody(), config.getStatusCode());
    }

    // --- Helper methods ---
    private boolean requiresBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }

    private boolean hasTemplate(String template) {
        return template != null && !template.isBlank();
    }

    private boolean validateFormatSafe(String requestBody, String template) {
        try {
            validateFormat(requestBody, template);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean bodyEquals(String template, String requestBody) {
        // Compare actual body with template
        try {
            JsonNode templateNode = objectMapper.readTree(template);
            JsonNode requestNode = objectMapper.readTree(requestBody);
            return templateNode.equals(requestNode);
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

    private void validateFormat(String actual, String template) {
        if (actual == null || template == null) return;

        MediaType type = detectMediaType(template);

        try {
            if (MediaType.APPLICATION_JSON.equals(type)) {
                objectMapper.readTree(actual);
            } else if (MediaType.APPLICATION_XML.equals(type)) {
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(actual)));
            }
        } catch (Exception e) {
            return;
        }
    }
}
