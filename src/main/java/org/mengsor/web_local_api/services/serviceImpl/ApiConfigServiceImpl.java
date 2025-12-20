package org.mengsor.web_local_api.services.serviceImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.mengsor.web_local_api.model.yaml.YamlFactory;
import org.mengsor.web_local_api.model.ApiConfig;
import org.mengsor.web_local_api.services.ApiConfigService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;
import org.yaml.snakeyaml.Yaml;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ApiConfigServiceImpl implements ApiConfigService {

    private final Path filePath;
    private final Yaml yaml = YamlFactory.create();
    private final List<ApiConfig> configs = new ArrayList<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ApiConfigServiceImpl(@Value("${api.config.cache.path}")  String path) {
        this.filePath = Path.of(path);
    }

    @Override
    public ApiConfig find(String version, String apiName) {
        return null;
    }

    @Override
    public ApiConfig findById(Long id) {
        ApiConfig apiConfig = loadFromFile().stream().filter(api -> api.getId().equals(id)).findFirst().orElse(null);

        return apiConfig != null ? apiConfig : new ApiConfig();
    }

    @Override
    public synchronized void save(ApiConfig apiConfig) {
        log.info("start save API config: {}", apiConfig);
        String requestBody = apiConfig.getRequestBody();
        String responseBody = apiConfig.getResponseBody();

        if (requestBody != null && !requestBody.isEmpty()) payloadValidator(apiConfig.getRequestFormat(), requestBody);
        if (responseBody != null && !responseBody.isEmpty()) payloadValidator(apiConfig.getResponseFormat(), responseBody);

        boolean updated = false;
        List<ApiConfig> apiConfigList = new ArrayList<>();

        if (Files.exists(filePath)) {
            apiConfigList = loadFromFile();
        }

        if (!apiConfigList.isEmpty()) {
            updated = apiConfigList.removeIf(a -> a.getId().equals(apiConfig.getId()))
                    && apiConfigList.add(apiConfig);
        }

        if (!updated) {
            apiConfigList.add(apiConfig);
        }

        writeToFile(apiConfigList);
    }


    @Override
    public void clear() {
       configs.clear();
    }

    @Override
    public synchronized List<ApiConfig> findAll(){
        return loadFromFile();
    }


    @Override
    public ApiConfig findByUrl(String url) {
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<ApiConfig> loadFromFile() {

        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (InputStream in = Files.newInputStream(filePath)) {

            Yaml yaml = YamlFactory.create();
            List<Map<String, Object>> raw = yaml.load(in);

            if (raw == null) {
                return new ArrayList<>();
            }

            List<ApiConfig> result = new ArrayList<>();

            for (Map<String, Object> map : raw) {
                result.add(mapToApi(map));
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ApiConfig mapToApi(Map<String, Object> map) {

        ApiConfig api = new ApiConfig();

        api.setId(map.get("id") == null ? null : Long.valueOf(map.get("id").toString()));
        api.setName((String) map.get("name"));
        api.setMethod((String) map.get("method"));
        api.setUrl((String) map.get("url"));
        api.setHeaders(List.class.cast(map.get("headers")));
        api.setResponseBody((String) map.get("responseBody"));
        api.setRequestBody((String) map.get("requestBody"));
        api.setRequestFormat((String) map.get("requestFormat"));
        api.setResponseFormat((String) map.get("responseFormat"));
        api.setStatusCode((Integer) map.get("statusCode"));

        return api;
    }

    private void writeToFile(List<ApiConfig> apis) {

        try {
            Files.createDirectories(filePath.getParent());

            try (Writer writer = Files.newBufferedWriter(filePath)) {
                yaml.dump(apis, writer);
                log.info("API configs saved to {}", filePath);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write API cache", e);
        }
    }

    private static void payloadValidator(String format, String body){
        log.info("Validating {} payload", format.toUpperCase());
        if (body == null || body.trim().isEmpty()) {
            return; // allow empty body
        }

        try {
            if ("json".equalsIgnoreCase(format)) {
                objectMapper.readTree(body);
                log.info("JSON payload is valid");
            } else if ("xml".equalsIgnoreCase(format)) {
                DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .parse(new InputSource(new StringReader(body)));
                log.info("XML payload is valid");
            } else {
                log.error("Unsupported format: {}", format);
                throw new IllegalArgumentException("Unsupported format: " + format);
            }
        } catch (Exception e) {
            log.error("Invalid {} payload: {}", format.toUpperCase(), body, e);
            throw new IllegalArgumentException(
                    "Invalid " + format.toUpperCase() + " payload", e
            );
        }
    }

}
