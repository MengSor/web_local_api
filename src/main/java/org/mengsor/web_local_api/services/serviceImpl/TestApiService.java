//package org.mengsor.web_local_api.services.serviceImpl;
//
//
//import jakarta.servlet.http.HttpServletRequest;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.mengsor.web_local_api.component.SoapUtils;
//import org.mengsor.web_local_api.model.ApiConfig;
//import org.mengsor.web_local_api.model.response.ApiResponse;
//import org.mengsor.web_local_api.services.ApiConfigService;
//import org.mengsor.web_local_api.services.RequestLogService;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.w3c.dom.Document;
//
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class TestApiService {
//
//    private final ApiConfigService apiConfigService;
//    private final RequestLogService requestLogService;
//    private final SoapUtils soapUtils;
//
//    public ApiResponse handleRequest(HttpServletRequest request, String requestBody) {
//
//        String path = request.getRequestURI().replaceFirst(".*/skyvva.api/", "");
//        String method = request.getMethod();
//
//        ApiConfig config = apiConfigService.findAll().stream()
//                .filter(api -> path.equalsIgnoreCase(api.getUrl())
//                        && method.equalsIgnoreCase(api.getMethod()))
//                .findFirst()
//                .orElse(null);
//
//        if (config == null) {
//            return new ApiResponse(false, "API not configured", null, 404);
//        }
//
//        boolean isSoap = soapUtils.isSoapRequest(request, requestBody);
//
//        try {
//            if (isSoap) {
//                soapUtils.validateSoapAction(request, config);
//                soapUtils.validateEnvelope(requestBody);
//
//                Document doc = soapUtils.parseSoap(requestBody);
//                soapUtils.validateSoapAuth(doc, config);
//                soapUtils.validateXPath(requestBody, config.getXpathRules());
//            }
//
//            String response = config.getResponseBody();
//            if (isSoap) {
//                response = soapUtils.wrapResponse(response);
//            }
//
//            requestLogService.logMatched(
//                    request, requestBody, config, response, config.getStatusCode());
//
//            return new ApiResponse(true, "Success", response, config.getStatusCode());
//
//        } catch (SoapValidationException ex) {
//
//            String fault = soapUtils.buildFault("soap:Client", ex.getMessage());
//
//            requestLogService.logUnmatched(
//                    request, requestBody, config, "SOAP Fault",
//                    ex.getMessage(), 500);
//
//            return new ApiResponse(false, ex.getMessage(), fault, 500);
//        }
//    }
//}
//
