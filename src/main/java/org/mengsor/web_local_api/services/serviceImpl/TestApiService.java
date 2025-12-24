package org.mengsor.web_local_api.services.serviceImpl;


import jakarta.servlet.http.HttpServletRequest;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TestApiService {

    public ApiResponse handleDynamicRequest(HttpServletRequest request, String body, Map<String, String> headers, boolean isSoap) {
        if (isSoap) {
            return handleSoapRequest(request, body, headers);
        } else {
            return handleRestRequest(request, body, headers);
        }
    }

    public ApiResponse handleRestRequest(HttpServletRequest request, String body, Map<String, String> headers) {
        // Implement your REST logic here
        // For example: route by path, method, or headers
        String response = "{\"status\":\"REST request processed successfully\"}";
        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .responseBody(response)
                .message("REST Success")
                .build();
    }

    public ApiResponse handleSoapRequest(HttpServletRequest request, String body, Map<String, String> headers) {
        // Implement your SOAP logic here
        // For example: parse SOAP envelope, route by SOAPAction, return XML
        String soapResponse = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
                "<soap:Body>" +
                "<Response>SOAP request processed successfully</Response>" +
                "</soap:Body>" +
                "</soap:Envelope>";

        return ApiResponse.builder()
                .status(HttpStatus.OK.value())
                .responseBody(soapResponse)
                .message("SOAP Success")
                .build();
    }
}

