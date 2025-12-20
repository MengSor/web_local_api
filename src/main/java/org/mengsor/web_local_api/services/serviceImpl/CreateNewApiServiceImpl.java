package org.mengsor.web_local_api.services.serviceImpl;

import org.mengsor.web_local_api.model.yaml.YamlFactory;
import org.mengsor.web_local_api.model.CreateNewApi;
import org.mengsor.web_local_api.services.CreateNewApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

@Service
public class CreateNewApiServiceImpl implements CreateNewApiService {

    private final Path filePath;
    private final Yaml yaml =   YamlFactory.create();
    private final List<CreateNewApi> mockApis = new ArrayList<>();
    @Value("${server.port}")
    private int serverPort;

    public CreateNewApiServiceImpl(@Value("${create.new.cache.path}") String path) {
        this.filePath = Path.of(path);
    }

    @Override
    public List<CreateNewApi> getAll() {
        return new ArrayList<>(mockApis); // Return a copy
    }

    @Override
    public synchronized void save(CreateNewApi api) {

        boolean updated = false;
        Long lastId = 0L;
        List<CreateNewApi> apis = new ArrayList<>();

        if (Files.exists(filePath)) {
            apis = loadFromFile();
            lastId = findLastId();
        }

        api.setId(lastId == 0 ? 1L : lastId + 1);
        api.setBaseUrl("http://localhost:"+serverPort+ "/skyvva.api/");
        api.setCreatedDate(new Date());

        if (!apis.isEmpty()) {
            for (int i = 0; i < apis.size(); i++) {
                String id = String.valueOf(apis.get(i).getId());
                if (id.equals(String.valueOf(api.getId()))) {
                    apis.set(i, api);
                    updated = true;
                    break;
                }
            }
        }

        if (!updated) {
            apis.add(api);
        }

        writeToFile(apis);
    }

    @Override
    public List<CreateNewApi> loadAll() {

        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (InputStream in = Files.newInputStream(filePath)) {

            List<CreateNewApi> list = yaml.loadAs(in, List.class);
            return list != null ? list : new ArrayList<>();

        } catch (Exception e) {
            throw new RuntimeException("Failed to load API cache", e);
        }
    }

    private void writeToFile(List<CreateNewApi> apis) {

        try {
            Files.createDirectories(filePath.getParent());

            try (Writer writer = Files.newBufferedWriter(filePath)) {
                yaml.dump(apis, writer);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to write API cache", e);
        }
    }



    @Override
    public Long findLastId() {
        List<CreateNewApi> apis = loadFromFile();
        return apis.stream()
                .map(CreateNewApi::getId)
                .filter(Objects::nonNull)
                .max(Long::compareTo)
                .orElse(0L);
    }

    @Override
    public synchronized void delete(Long id) {
        List<CreateNewApi> apis = loadFromFile();
        apis.removeIf(a -> a.getId().equals(id));
        writeToFile(apis);
    }

    @Override
    public void clear() {
        mockApis.clear();
    }

    @SuppressWarnings("unchecked")
    private List<CreateNewApi> loadFromFile() {

        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (InputStream in = Files.newInputStream(filePath)) {

            Yaml yaml = YamlFactory.create();
            List<Map<String, Object>> raw = yaml.load(in);

            if (raw == null) {
                return new ArrayList<>();
            }

            List<CreateNewApi> result = new ArrayList<>();

            for (Map<String, Object> map : raw) {
                result.add(mapToApi(map));
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CreateNewApi mapToApi(Map<String, Object> map) {

        CreateNewApi api = new CreateNewApi();

        api.setId(map.get("id") == null ? null : Long.valueOf(map.get("id").toString()));
        api.setName((String) map.get("name"));
        api.setBaseUrl((String) map.get("baseUrl"));
        api.setProtocol((String) map.get("protocol"));

        if (map.get("createdDate") != null) {
            api.setCreatedDate(map.get("createdDate") instanceof Date ? (Date) map.get("createdDate")
                    : Date.from(Instant.parse((String) map.get("createdDate"))));
        }

        return api;
    }

}
