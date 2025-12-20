package org.mengsor.web_local_api.services;

import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.response.ApiResponse;

public interface DynamicApiService {
    ApiResponse handleRequest(HttpServletRequest request, String requestBody);
}
