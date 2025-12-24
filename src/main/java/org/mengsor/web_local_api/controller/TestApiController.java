package org.mengsor.web_local_api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.mengsor.web_local_api.services.serviceImpl.TestApiService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/soap.api")
public class TestApiController {

    private final TestApiService dynamicApiService;

    public TestApiController(TestApiService dynamicApiService) {
        this.dynamicApiService = dynamicApiService;
    }

    @RequestMapping(value = "/**",
            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE},
            consumes = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE,
                    "application/soap+xml",
                    "text/xml"
            },
            produces = {
                    MediaType.APPLICATION_JSON_VALUE,
                    MediaType.APPLICATION_XML_VALUE,
                    "application/soap+xml",
                    "text/xml"
            })
    public ResponseEntity<String> execute(
            HttpServletRequest request,
            @RequestBody(required = false) String requestBody) {

        try {
            // Extract headers
            Map<String, String> headers = Collections.list(request.getHeaderNames())
                    .stream()
                    .collect(Collectors.toMap(
                            h -> h,
                            request::getHeader
                    ));

            String soapAction = request.getHeader("SOAPAction");
            String b = requestBody.replaceAll(">\\s+<", "><")
                    .replaceAll("\\r|\\n", "")
                    .trim();
            // Determine if SOAP request
            boolean isSoap = b != null && b.contains("<soap:Envelope");

            ApiResponse response = dynamicApiService.handleDynamicRequest(request, b, headers, isSoap);

            // Set response content type dynamically
            MediaType contentType = isSoap ? MediaType.parseMediaType("application/soap+xml") : MediaType.APPLICATION_JSON;

            return ResponseEntity.status(response.getStatus())
                    .contentType(contentType)
                    .body(response.getResponseBody() != null ? response.getResponseBody() : response.getMessage());

        } catch (Exception e) {
            // Handle SOAP Fault
            String faultResponse = "<soap:Fault>" +
                    "<faultcode>Server</faultcode>" +
                    "<faultstring>" + e.getMessage() + "</faultstring>" +
                    "</soap:Fault>";

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.parseMediaType("application/soap+xml"))
                    .body(faultResponse);
        }
    }
}
