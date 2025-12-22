package org.mengsor.web_local_api.component;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.model.enums.BodyType;
import org.mengsor.web_local_api.services.RequestLogService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;

/**
 * @author mengsor
 * @Date 2025/12/22
 * The RequestMismatchReporter class provides functionality for comparing incoming HTTP requests
 * against expected API configurations and reporting mismatches. Mismatches are determined
 * based on discrepancies in request method, URL, headers, and body content. The class includes
 * mechanisms for normalizing, formatting, and comparing JSON, XML, and text-based content.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RequestMismatchReporter {

    private static final int COL_WIDTH = 58;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestLogService requestLogService;

    /**
     * Builds a non-match report for the given HTTP request by comparing it against the provided API configuration,
     * including differences in method, path, headers, and body.
     *
     * @param request the HttpServletRequest representing the incoming request
     * @param config the ApiConfig containing the expected configuration for the request
     * @param actualBody the actual body of the request as a String
     * @param headerDiffs a list of String representing the differences between expected and actual headers
     * @return a formatted String containing a detailed report of mismatches between the request and the configuration
     */
    public String buildNonMatchReport(HttpServletRequest request,
                                       ApiConfig config,
                                       String actualBody, List<String> headerDiffs) {

        log.info("start buildNonMatchReport request:{}",request.getRequestURI());
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("%70s","Request was not matched\n"));
        sb.append(String.format("%70s","======================\n\n"));

        sb.append("----------------------------------------------------------------" +
                "-------------------------------------------------------\n");
        sb.append(String.format("| %-55s | %-55s |\n", "Closest stub", "Request"));
        sb.append("----------------------------------------------------------------" +
                "-------------------------------------------------------\n");

        /* ---------- METHOD ---------- */
        sb.append(String.format("| %-55s | %-55s |\n", config.getMethod(), request.getMethod()));

        /* ---------- PATH ---------- */
        String actualPath = request.getRequestURI().replaceFirst(".*/skyvva.api/", "");
        sb.append(String.format("| %-55s | %-55s |\n", "[path] " + config.getUrl(), actualPath));

        /* ---------- HEADERS ---------- */
        for (String diff : headerDiffs) {
            sb.append("| ").append(diff).append(" |\n");
        }

        sb.append("--------------------------------------------------------" +
                "---------------------------------------------------------------\n");

        /* ---------- BODY ---------- */
        if (config.getRequestBody() != null && !config.getRequestBody().isBlank() && !bodyEquals(actualBody, config.getRequestBody())) {
            sb.append("\nBody does not match\n");
            sb.append("-------------------------------------------------------" +
                    "----------------------------------------------------------------\n");
            if (actualBody == null || actualBody.isBlank()) {
                BodyType type = detectBodyType(config.getRequestBody());

                if (BodyType.JSON.equals(type)) {
                    appendJsonDiffLikeXml(sb,
                            config.getRequestBody(),
                            actualBody
                    );
                } else if (BodyType.XML.equals(type)) {
                    appendXmlDiff(
                            sb,
                            config.getRequestBody(),
                            actualBody
                    );
                }
            } else if (!bodyEquals(config.getRequestBody(), actualBody)) {
                BodyType type = detectBodyType(config.getRequestBody());
                BodyType actualType = detectBodyType(actualBody);

                if (type.equals(actualType)) {
                    if (type.equals(BodyType.JSON)) {
                        appendJsonDiffLikeXml(
                                sb,
                                config.getRequestBody(),
                                actualBody
                        );
                    } else if (type.equals(BodyType.XML)) {
                        appendXmlDiff(
                                sb,
                                config.getRequestBody(),
                                actualBody
                        );
                    }
                }else {
                    log.warn("buildNonMatchReport Type different");
                    String prettyActualBody = BodyType.JSON.equals(actualType) ? prettyJson(actualBody) : prettyXml(actualBody);
                    String prettyExpectedBody = BodyType.JSON.equals(type) ? prettyJson(config.getRequestBody()) : prettyXml(config.getRequestBody());
                    appendRawSideBySide(sb,prettyExpectedBody,prettyActualBody,type,actualType);
                }

            }
        }

        log.info("buildNonMatchReport successfully sb:{}",sb.toString());
        return sb.toString();
    }

    /**
     * Compares the headers from the given HTTP request against the expected headers
     * and identifies differences. Differences could include missing headers or headers
     * with mismatched values.
     *
     * @param request the HttpServletRequest containing the actual headers to compare
     * @param expectedHeaders a list of expected header definitions, each represented by an instance
     *                        of a compatible type (e.g., ApiConfig.Header)
     * @return a list of strings describing the differences between the actual and expected headers.
     *         Each string provides detailed information about the discrepancy, such as whether a
     *         header is missing or contains a mismatched value.
     */
    public List<String> compareHeaders(HttpServletRequest request,
                                        List<?> expectedHeaders) {

        log.info("start compareHeaders request:{}",request.getRequestURI());
        List<String> diffs = new ArrayList<>();
        Map<String, String> header = extractHeaders(request);

        if (expectedHeaders == null || expectedHeaders.isEmpty()) {
            return diffs;
        }

        ObjectMapper mapper = new ObjectMapper();

        for (Object obj : expectedHeaders ) {

            ApiConfig.Header expected =
                    mapper.convertValue(obj, ApiConfig.Header.class);

            String actualValue = header.entrySet()
                    .stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(expected.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);

            if (actualValue == null) {
                diffs.add(
                        String.format(
                                "%-55s | %-55s <<<<< Header missing",
                                expected.getKey() + " [contains] : " + expected.getValue(),
                                "-"
                        )
                );
            }
            else if (!actualValue.equals(expected.getValue())) {
                diffs.add(
                        String.format(
                                "%-55s | %-55s <<<<< Header does not match",
                                expected.getKey() + " [contains] : " + expected.getValue(),
                                expected.getKey() + ": " + actualValue
                        )
                );
            }
        }

        log.info("compareHeaders successfully diffs:{}",diffs);
        return diffs;
    }

    /**
     *
     * @param request
     * @return
     */
    private Map<String, String> extractHeaders(HttpServletRequest request) {

        Map<String, String> headers = new LinkedHashMap<>();

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames == null) {
            return headers;
        }

        while (headerNames.hasMoreElements()) {
            String name = headerNames.nextElement();
            headers.put(name, request.getHeader(name));
        }

        return headers;
    }

    /**
     *
     * @param sb
     * @param expectedJson
     * @param actualJson
     */
    private void appendJsonDiffLikeXml(StringBuilder sb,
                                       String expectedJson,
                                       String actualJson) {

        sb.append("[equalToJson]\n");

        List<String> expected = normalizeJsonLines(expectedJson);
        List<String> actual = normalizeJsonLines(actualJson);

        int max = Math.max(expected.size(), actual.size());
        int index = 1;
        for (int i = 0; i < max; i++) {
            String exp = i < expected.size() ? expected.get(i) : "";
            String act = i < actual.size() ? actual.get(i) : "";

            boolean mismatch = !exp.trim().equals(act.trim());

            sb.append(String.format(
                    "%-58s | %-58s",
                    exp, act
            ));

            if (mismatch && index == max) {
                sb.append("   <<<<< Body does not match");
            }
            index++;
            sb.append("\n");
        }
    }

    /**
     *
     * @param json
     * @return
     */
    private List<String> normalizeJsonLines(String json) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(json);

            ObjectWriter writer = mapper.writer(
                    new DefaultPrettyPrinter()
                            .withObjectIndenter(
                                    new DefaultIndenter("  ", "\n")
                            )
            );

            String pretty = writer.writeValueAsString(node);
            return Arrays.asList(pretty.split("\n"));

        } catch (Exception e) {
            return Arrays.asList(json.split("\n"));
        }
    }

    /**
     *
     * @param sb
     * @param expectedXml
     * @param actualXml
     */
    private void appendXmlDiff(StringBuilder sb,
                               String expectedXml,
                               String actualXml) {

        sb.append("[equalToXml]\n");

        List<String> expected = normalizeXmlLines(expectedXml == null ? "" : expectedXml);
        List<String> actual = normalizeXmlLines(actualXml == null ? "" : actualXml);

        int max = Math.max(expected.size(), actual.size());
        int index = 1;
        for (int i = 0; i < max; i++) {
            String exp = i < expected.size() ? expected.get(i) : "";
            String act = i < actual.size() ? actual.get(i) : "";

            boolean mismatch = !exp.equals(act);

            sb.append(String.format(
                    "%-58s | %-58s",
                    exp, act
            ));

            if (mismatch && index == max) {
                sb.append("   <<<<< Body does not match");
            }
            index++;
            sb.append("\n");
        }
    }

    /**
     *
     * @param xml
     * @return
     */
    private List<String> normalizeXmlLines(String xml) {

        log.info("normalizeXmlLines xml:{}",xml);
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setCoalescing(true);
            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(new StringReader(xml)));
            doc.normalizeDocument();

            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(
                    "{http://xml.apache.org/xslt}indent-amount", "2"
            );
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(doc), new StreamResult(writer));

            return Arrays.stream(writer.toString().split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();

        } catch (Exception e) {
            return Arrays.stream(xml.split("\n"))
                    .map(String::trim)
                    .filter(line -> !line.isBlank())
                    .toList();
        }
    }

    /**
     * Appends a side-by-side comparison of the expected and actual text content to the provided StringBuilder.
     * The output includes aligned, trimmed, and filtered lines from both texts, with headers showing their respective types.
     * It concludes with an indicator for a body mismatch if any discrepancies are found.
     *
     * @param sb the StringBuilder to append the comparison output to
     * @param expected the expected text content
     * @param actual the actual text content
     * @param expectedType the type of the expected text content (e.g., JSON, XML, TEXT)
     * @param actualType the type of the actual text content (e.g., JSON, XML, TEXT)
     */
    private void appendRawSideBySide(StringBuilder sb,
                                     String expected,
                                     String actual,
                                     BodyType expectedType,
                                     BodyType actualType) {


        log.info("appendRawSideBySide expected:{}",expected);
        sb.append(String.format("[equalTo%s]%"+(COL_WIDTH - ("[equalTo"+expectedType+"]").length())+"s| " +
                        "[actual%s]\n", expectedType, "", actualType));

        List<String> left  = Arrays.stream(expected.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();

        List<String> right = Arrays.stream(actual.split("\n"))
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();

        int max = Math.max(left.size(), right.size());

        for (int i = 0; i < max; i++) {
            String l = i < left.size() ? left.get(i) : "";
            String r = i < right.size() ? right.get(i) : "";

            sb.append(String.format(
                    "%-" + COL_WIDTH + "s | %-" + COL_WIDTH + "s%n",
                    l, r
            ));
        }

        sb.append(String.format(
                "%" + (COL_WIDTH * 2 + 3) + "s%n",
                "<<<<< Body does not match"
        ));
    }

    private String prettyJson(String json) {
        log.info("prettyJson json:{}",json);
        log.warn("prettyJson json:{}",json.replaceAll("\"[^\"]*\"", ""));
        try {
            ObjectMapper mapper = new ObjectMapper();
            Object obj = mapper.readValue(json, Object.class);
            return mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(obj);
        } catch (Exception e) {
            return json; // fallback
        }
    }

    private String prettyXml(String xml) {
        log.info("prettyXml xml:{}",xml);
        log.warn("prettyXml xml:{}",xml.replaceAll("<[^>]+>", ""));
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            StreamSource source = new StreamSource(new StringReader(xml));
            StringWriter writer = new StringWriter();
            transformer.transform(source, new StreamResult(writer));
            return writer.toString().trim();
        } catch (Exception e) {
            return xml;
        }
    }

    private BodyType detectBodyType(String body) {
        if (body == null || body.isBlank()) return BodyType.TEXT;

        String t = body.trim();
        if (t.startsWith("{")) return BodyType.JSON;
        if (t.startsWith("<")) return BodyType.XML;

        return BodyType.TEXT;
    }

    public boolean bodyEquals(String template, String requestBody) {
        if (template == null || requestBody == null) {
            return false;
        }

        String t = template.trim();
        String a = requestBody.trim();

        try {
            // ---------- JSON ----------
            if (t.startsWith("{") && a.startsWith("{")) {
                JsonNode expected = objectMapper.readTree(t);
                JsonNode actual = objectMapper.readTree(a);
                return expected.equals(actual);
            }

            // ---------- XML ----------
            if (t.startsWith("<") && a.startsWith("<")) {
                return xmlEquals(t, a);
            }

            // ---------- Plain text ----------
            return t.equals(a);

        } catch (Exception e) {
            return false;
        }
    }

    private boolean xmlEquals(String expectedXml, String actualXml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            factory.setNamespaceAware(true);
            factory.setIgnoringElementContentWhitespace(true);
            factory.setCoalescing(true);
            factory.setIgnoringComments(true);

            DocumentBuilder builder = factory.newDocumentBuilder();

            Document expectedDoc = builder.parse(
                    new InputSource(new StringReader(expectedXml))
            );
            Document actualDoc = builder.parse(
                    new InputSource(new StringReader(actualXml))
            );

            expectedDoc.normalizeDocument();
            actualDoc.normalizeDocument();

            return expectedDoc.isEqualNode(actualDoc);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     *
     * @param actual
     * @param template
     */
    public void validateFormat(String actual, String template) {
        log.info("validateFormat actual:{}",actual);
        if (actual == null || template == null) return;

        BodyType type = detectBodyType(template);

        try {
            if (BodyType.JSON.equals(type)) {
                objectMapper.readTree(actual);
            } else if (BodyType.XML.equals(type)) {
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(actual)));
            }
        } catch (Exception e) {
            requestLogService.logFailed(null, actual, "Invalid format");
           return;
        }
    }
}
