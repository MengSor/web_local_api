package org.mengsor.web_local_api.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiConfig {

    private Long id;
    private String name;
    private String url;        // v1/user-profile
    private String method;     // GET, POST, PUT, DELETE

    private List<Header> headers = new ArrayList<>();

    private String requestFormat = "json";   // json | xml
    private String responseFormat = "json";
    private String requestBody;
    private String responseBody;

    private int statusCode;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Header {
        private String key;
        private String value;
    }
}
