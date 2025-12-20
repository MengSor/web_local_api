package org.mengsor.web_local_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.mengsor.web_local_api.services.DynamicApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/skyvva.api")
public class DynamicApiController {

    private final DynamicApiService dynamicApiService;

    public DynamicApiController(DynamicApiService dynamicApiService) {
        this.dynamicApiService = dynamicApiService;
    }

    @RequestMapping(value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
    public ResponseEntity<String> execute(
            HttpServletRequest request,
            @RequestBody(required = false) String requestBody) {

        ApiResponse response = dynamicApiService.handleRequest(request, requestBody);

        return ResponseEntity.status(response.getStatus())
                .body(response.getResponseBody() != null ? response.getResponseBody() : response.getMessage());
    }
}
