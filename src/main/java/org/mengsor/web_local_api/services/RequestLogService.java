package org.mengsor.web_local_api.services;

import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.RequestLog;

import java.util.List;

public interface RequestLogService {
    void logMatched(HttpServletRequest request, String requestBody, ApiConfig config);
    List<RequestLog> getLogs();
    void saveLog(String url, String method, String requestBody, String responseBody, int status, long duration);
    void clearAllLogs();
    RequestLog getLogById(Long id);
    List<RequestLog> getAllLogs();
    void logFailed(HttpServletRequest request, String requestBody, String errorMessage);
}
