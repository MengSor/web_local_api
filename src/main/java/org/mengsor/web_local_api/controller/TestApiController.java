//package org.mengsor.web_local_api.controller;
//
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import org.mengsor.web_local_api.component.SoapUtils;
//import org.mengsor.web_local_api.model.response.ApiResponse;
//import org.mengsor.web_local_api.services.serviceImpl.TestApiService;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.MediaType;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestMethod;
//import org.springframework.web.bind.annotation.RestController;
//
//
//@RestController
//@RequestMapping("/skyvva.api")
//@RequiredArgsConstructor
//public class TestApiController {
//
//    private final TestApiService testApiService;
//    private final SoapUtils soapUtils;
//
//    @RequestMapping(value = "/**",
//            method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
//    public ResponseEntity<String> execute(
//            HttpServletRequest request,
//            @RequestBody(required = false) String body) {
//
//        ApiResponse response = testApiService.handleRequest(request, body);
//
//        boolean isSoap = soapUtils.isSoapRequest(request, body);
//
//        return ResponseEntity.status(response.getStatus())
//                .contentType(isSoap ? MediaType.TEXT_XML : MediaType.APPLICATION_JSON)
//                .body(response.getResponseBody() != null
//                        ? response.getResponseBody()
//                        : response.getMessage());
//    }
//}
