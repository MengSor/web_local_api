package org.mengsor.web_local_api.model;

import lombok.Data;
import org.mengsor.web_local_api.model.enums.MatchStatus;

import java.util.Map;

@Data
public class RequestLog {
    private String id;
    private String url;
    private String method;
    private String clientIp;
    private String requestBody;
    private String responseBody;
    private String expectedRequestBody;  // new
    private MatchStatus matchStatus;
    private int status;
    private long duration;
    private String timestamp;
    private Map<String, String> headers;
    private String nonMatchReport;
    private int statusCode;
    private String apiName;

    public boolean isMatched() {
        return MatchStatus.MATCHED.equals(matchStatus);
    }

    public boolean isError() {
        return status >= 400;
    }
}
