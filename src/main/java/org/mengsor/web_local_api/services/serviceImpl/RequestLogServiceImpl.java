package org.mengsor.web_local_api.services.serviceImpl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.RequestLog;
import org.mengsor.web_local_api.model.enums.MatchStatus;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RequestLogServiceImpl implements RequestLogService {

    private final List<RequestLog> logs = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public void logMatched(HttpServletRequest request, String requestBody, ApiConfig config) {
        RequestLog log = createLog(request, requestBody, config.getResponseBody(),
                config.getRequestBody(), MatchStatus.MATCHED, config.getStatusCode());
        logs.add(log);
    }

    @Override
    public List<RequestLog> getLogs() {
        return logs;
    }

    @Override
    public void saveLog(String url, String method, String requestBody, String responseBody, int status, long duration) {
        RequestLog log = new RequestLog();
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT+7"));
        log.setId(System.currentTimeMillis()); // simple ID
        log.setUrl(url);
        log.setMethod(method);
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);
        log.setStatus(status);
        log.setDuration(duration);
        log.setTimestamp(String.valueOf(zonedDateTime.toLocalDateTime()));

        logs.add(log);
    }

    @Override
    public void clearAllLogs() {
        logs.clear();
    }

    @Override
    public RequestLog getLogById(Long id) {
        return logs.stream().filter(l -> l.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public List<RequestLog> getAllLogs() {
        log.info("Returning {} logs", logs.size());
        return new ArrayList<>(logs);
    }

    @Override
    public void logFailed(HttpServletRequest request, String requestBody, String errorMessage) {
        RequestLog log = createLog(request, requestBody, null, null, MatchStatus.UNMATCHED,
                HttpStatus.BAD_REQUEST.value());
        log.setResponseBody(errorMessage); // store error message in responseBody
        logs.add(log);
    }

    private RequestLog createLog(HttpServletRequest request, String requestBody, String responseBody,
                                 String expectedRequestBody, MatchStatus status, int httpStatus) {
        ZonedDateTime zonedDateTime = ZonedDateTime.now(ZoneId.of("GMT+7"));
        LocalDateTime now = LocalDateTime.now(ZoneId.of("GMT+7"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSSSSS");
        String formatted = now.format(formatter);
        RequestLog log = new RequestLog();
        log.setId(System.currentTimeMillis()); // simple ID for example
        log.setUrl(request.getRequestURI());
        log.setMethod(request.getMethod());
        log.setClientIp(request.getRemoteAddr());
        log.setRequestBody(requestBody);
        log.setResponseBody(responseBody);
        log.setExpectedRequestBody(expectedRequestBody);
        log.setMatchStatus(status);
        log.setStatus(httpStatus);
        log.setTimestamp(formatted);
        log.setDuration(0); // can compute duration if needed
        log.setHeaders(Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, request::getHeader)));
        return log;
    }

    public Optional<RequestLog> getLogByIds(Long id) {
        return logs.stream().filter(log -> log.getId().equals(id)).findFirst();
    }
}
