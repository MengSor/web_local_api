package org.mengsor.web_local_api.services.serviceImpl;


import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.component.RequestMismatchReporter;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.response.ApiResponse;
import org.mengsor.web_local_api.services.ApiConfigService;
import org.mengsor.web_local_api.services.DynamicApiService;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DynamicApiServiceImpl implements DynamicApiService {

    private final ApiConfigService apiConfigService;
    private final RequestLogService requestLogService;
    private final RequestMismatchReporter reporter;

    public DynamicApiServiceImpl(ApiConfigService apiConfigService,
                                 RequestLogService requestLogService,
                                 RequestMismatchReporter reporter) {
        this.apiConfigService = apiConfigService;
        this.requestLogService = requestLogService;
        this.reporter = reporter;
    }

    @Override
    public ApiResponse handleRequest(HttpServletRequest request, String requestBody) {

        log.info("Received request: {} {}", request.getMethod(), request.getRequestURI());
        String apiPath = request.getRequestURI().replaceFirst(".*/query.api/", "");
        String method = request.getMethod();

        // Get query parameters
        Map<String, String[]> queryParams = request.getParameterMap();

        // Get cookies
        Map<String, String> cookies = new HashMap<>();
        if (request.getCookies() != null) {
            Arrays.stream(request.getCookies())
                    .forEach(c -> cookies.put(c.getName(), c.getValue()));
        }

        ApiConfig config = apiConfigService.findAll().stream()
                .filter(api -> apiPath.equalsIgnoreCase(api.getUrl()) &&
                        method.equalsIgnoreCase(api.getMethod()))
                .findFirst()
                .orElse(null);

        if (config == null) {
            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    null,
                    "API not configured",
                    "No matching API found for " + method + " " + apiPath,
                    HttpStatus.NOT_FOUND.value()
            );
            log.error("API not configured: {} {}", method, apiPath);
            return new ApiResponse(false, "API not configured: " + method + " " + apiPath,
                    null, HttpStatus.NOT_FOUND.value());
        }

        boolean isSoap = "SOAP".equalsIgnoreCase(config.getProtocol());

        // --- Validate query parameters ---
        List<ApiConfig.keyValuePair> queryList = convertToKeyValuePairList(config.getQueries());
        Map<String, String> requiredQueries = toMap(queryList);
        List<String> missingQueries = requiredQueries.entrySet().stream()
                .filter(e -> queryParams.get(e.getKey()) == null || !e.getValue().equals(queryParams.get(e.getKey())[0]))
                .map(Map.Entry::getKey).toList();
        if (!missingQueries.isEmpty()) {
            String msg = "Missing or invalid query parameters: " + String.join(", ", missingQueries);
            requestLogService.logUnmatched(request, requestBody, config, "Missing/Invalid query parameters",
                    msg, HttpStatus.BAD_REQUEST.value());
            log.error(msg);
            return new ApiResponse(false, msg, null, HttpStatus.BAD_REQUEST.value());
        }

        // --- Validate cookies ---
        List<ApiConfig.keyValuePair> cookieList = convertToKeyValuePairList(config.getCookies());
        Map<String, String> requiredCookies = toMap(cookieList);
        List<String> missingCookies = requiredCookies.entrySet().stream()
                .filter(e -> !e.getValue().equals(cookies.get(e.getKey())))
                .map(Map.Entry::getKey).toList();

        if (!missingCookies.isEmpty()) {
            String msg = "Missing or invalid cookies: " + String.join(", ", missingCookies);
            requestLogService.logUnmatched(request, requestBody, config, "Missing/Invalid cookies", msg,
                    HttpStatus.BAD_REQUEST.value());
            log.error(msg);
            return new ApiResponse(false, msg, null, HttpStatus.BAD_REQUEST.value());
        }


        // Validate Request Body
        if (!validateFormatSafe(requestBody, config.getRequestBody())) {
            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    config,
                    "Request body format invalid",
                    null,
                    HttpStatus.BAD_REQUEST.value()
            );
            log.error("Request body format invalid: {}", requestBody);
            return new ApiResponse(false, "Request body format invalid", null,
                    HttpStatus.BAD_REQUEST.value());
        }

        Map<String, String> soapHeaders = new HashMap<>();
        String extractedBody = requestBody;

        // --- SOAP Validation ---
        if (isSoap) {
            try {
                if (!config.getRequestBody().isBlank() && config.getRequestBody() != null) {
                    SoapValidationResult result = validateSoapRequestWithHeaders(request, requestBody);
                    extractedBody = result.getExtractedBody();
                    soapHeaders = result.getHeaders();
                }
            } catch (Exception ex) {
               int statusCode = ex instanceof RuntimeException ? HttpStatus.INTERNAL_SERVER_ERROR.value() : HttpStatus.BAD_REQUEST.value();
               String valueString = statusCode == 500 ? "Internal Server Error" : "Bad Request";
               String message = ex.getMessage();
                String fault = buildSoapFault(statusCode,valueString,message);
                log.error("SOAP validation failed: {}", ex.getMessage());
                log.debug("SOAP request body: {}", fault);
                requestLogService.logUnmatched(request, requestBody, config, "SOAP validation failed",
                        fault, statusCode);
                return new ApiResponse(false, ex.getMessage(), fault, statusCode);
            }
        }

        // Validate content
        List<String> headerDiffs = new ArrayList<>();

        if ((config.getHeaders() != null && !config.getHeaders().isEmpty()) && config.getHeaders().size() > 0) {
            headerDiffs  = reporter.compareHeaders(request, config.getHeaders());
        }
        if ((hasTemplate(config.getRequestBody()) && !reporter.bodyEquals(config.getRequestBody(), requestBody))
             || (config.getHeaders()!=null && headerDiffs.size() >0 && !headerDiffs.isEmpty())) {

            String diffReport = reporter.buildNonMatchReport(request, config, requestBody, headerDiffs);
            String fault = null;
            if (isSoap){
                fault = buildSoapFault(HttpStatus.NOT_FOUND.value(),"Record not found","The requested record could not be found.");
            }

            requestLogService.logUnmatched(
                    request,
                    requestBody,
                    config,
                    "Request does not match template",
                    diffReport,
                    HttpStatus.NOT_FOUND.value()
            );
            log.error("Request does not match template: {}", requestBody);
            return new ApiResponse(false, "Request does not match template", isSoap ? fault : diffReport,
                    HttpStatus.NOT_FOUND.value());
        }

        // Prepare response
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(detectMediaType(config.getResponseBody()));

        String responseBody = config.getResponseBody();

        if (isSoap && responseBody != null && !responseBody.contains("Envelope")) {
            responseBody = wrapSoapResponse(responseBody, soapHeaders);
        }

        String body = isSoap ? responseBody : config.getResponseBody();
        // Log success
        requestLogService.logMatched(
                request,
                requestBody,
                config,
                 body,
                config.getStatusCode()
        );

        log.info("Received request successfully");
        return new ApiResponse(true, "Success", body, config.getStatusCode());
    }

    // --- Helper methods ---
    private boolean requiresBody(String method) {
        return "POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method);
    }

    private boolean hasTemplate(String template) {
        log.debug("Checking if template is present: {}", template);
        return template != null && !template.isBlank();
    }

    private boolean validateFormatSafe(String requestBody, String template) {
        log.debug("Validating format of request body: {} against template: {}", requestBody, template);
        try {
            reporter.validateFormat(requestBody, template);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private MediaType detectMediaType(String body) {
        if (body == null) return MediaType.TEXT_PLAIN;
        String t = body.trim();
        if (t.startsWith("{")) return MediaType.APPLICATION_JSON;
        if (t.startsWith("<")) return MediaType.APPLICATION_XML;
        return MediaType.TEXT_PLAIN;
    }

    private Map<String, String> toMap(List<ApiConfig.keyValuePair> list) {
        if (list == null) return Collections.emptyMap();
        return list.stream().collect(Collectors.toMap(ApiConfig.keyValuePair::getKey,
                ApiConfig.keyValuePair::getValue));
    }

    private List<ApiConfig.keyValuePair> convertToKeyValuePairList(List<?> list) {
        if (list == null) return Collections.emptyList();
        ObjectMapper mapper = new ObjectMapper();
        return list.stream()
                .map(obj -> mapper.convertValue(obj, ApiConfig.keyValuePair.class))
                .collect(Collectors.toList());
    }

    // --- SOAP validation with WS-Security ---
    private SoapValidationResult validateSoapRequestWithHeaders(HttpServletRequest request, String body) throws Exception {
        if (body == null || body.isBlank()) throw new RuntimeException("SOAP body is empty");


        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document doc = factory.newDocumentBuilder().parse(new InputSource(new StringReader(body)));

        Map<String, String> headersMap = new HashMap<>();

        // --- Extract headers ---
        NodeList headerNodes = doc.getElementsByTagNameNS("*", "Header");
        if (headerNodes.getLength() > 0) {
            NodeList children = headerNodes.item(0).getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    headersMap.put(child.getLocalName(), child.getTextContent().trim());

                    // --- WS-Security validation ---
                    if ("Security".equals(child.getLocalName())) {
                        validateWSSecurity(child);
                    }
                }
            }
        }

        // --- Extract body payload ---
        NodeList bodyNodes = doc.getElementsByTagNameNS("*", "Body");
        if (bodyNodes.getLength() == 0) throw new RuntimeException("SOAP Body not found");
        Node bodyNode = bodyNodes.item(0).getFirstChild();
        if (bodyNode == null) throw new RuntimeException("SOAP Body is empty");

        StringWriter sw = new StringWriter();
        Transformer transformer = TransformerFactory.newInstance().newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        transformer.transform(new DOMSource(bodyNode), new StreamResult(sw));

        return SoapValidationResult.valid(sw.toString(), headersMap);
    }

    // --- WS-Security validation ---
    private void validateWSSecurity(Node securityNode) {
        NodeList children = securityNode.getChildNodes();
        String username = null;
        String password = null;

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("UsernameToken".equals(child.getLocalName())) {
                NodeList tokenChildren = child.getChildNodes();
                for (int j = 0; j < tokenChildren.getLength(); j++) {
                    Node tokenChild = tokenChildren.item(j);
                    if ("Username".equals(tokenChild.getLocalName())) {
                        username = tokenChild.getTextContent().trim();
                    } else if ("Password".equals(tokenChild.getLocalName())) {
                        password = tokenChild.getTextContent().trim();
                    }
                }
            }
        }

        if (username == null || password == null) {
            throw new RuntimeException("Missing WS-Security Username or Password");
        }

        // --- Mock credentials (replace with DB/config if needed) ---
        if (!"admin".equals(username) || !"admin123".equals(password)) {
            throw new RuntimeException("Invalid WS-Security credentials");
        }
    }

    // --- SOAP response wrapper ---
    private String wrapSoapResponse(String body, Map<String, String> headers) {
        StringBuilder headerXml = new StringBuilder();
        if (headers != null && !headers.isEmpty()) {
            headers.forEach((k, v) -> headerXml.append("<").append(k).append(">")
                    .append(v)
                    .append("</").append(k).append(">"));
        }
        return """
        <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
            <soapenv:Header>
                %s
            </soapenv:Header>
            <soapenv:Body>
                %s
            </soapenv:Body>
        </soapenv:Envelope>
        """.formatted(headerXml, body);
    }

    // --- SOAP Fault ---
    private String buildSoapFault(int status,String valueString, String message) {
        return """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
                  <soapenv:Body>
                    <soapenv:Fault>
                      <faultcode>soapenv:Client</faultcode>
                      <faultstring>%s</faultstring>
                      <detail>
                        <error>
                          <code>%s</code>
                          <message>%s</message>
                        </error>
                      </detail>
                    </soapenv:Fault>
                  </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(
                xmlEscape(valueString),
                xmlEscape(String.valueOf(status)),
                xmlEscape(message)
        );

    }

    private static String xmlEscape(String value) {
        return value == null ? "" :
                value.replace("&", "&amp;")
                        .replace("<", "&lt;")
                        .replace(">", "&gt;")
                        .replace("\"", "&quot;")
                        .replace("'", "&apos;");
    }


    // --- SOAP Validation Result ---
    private record SoapValidationResult(String extractedBody, Map<String, String> headers) {
        static SoapValidationResult valid(String extractedBody, Map<String, String> headers) {
            return new SoapValidationResult(extractedBody, headers);
        }
        String getExtractedBody() { return extractedBody; }
        Map<String, String> getHeaders() { return headers; }
    }
}
