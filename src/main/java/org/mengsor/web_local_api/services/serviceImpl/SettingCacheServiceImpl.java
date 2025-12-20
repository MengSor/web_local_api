package org.mengsor.web_local_api.services.serviceImpl;

import org.mengsor.web_local_api.configuration.until.CryptoUtil;
import org.mengsor.web_local_api.model.SettingCache;
import org.mengsor.web_local_api.model.enums.SecurityMode;
import org.mengsor.web_local_api.services.SettingCacheService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SettingCacheServiceImpl implements SettingCacheService {

    private final Path filePath;
    private final Yaml yaml = new Yaml();

    public SettingCacheServiceImpl(@Value("${setting.cache.path}") String path) {
        this.filePath = Paths.get(path);
    }

    @Override
    public synchronized void save(SettingCache cache) {
        // Encrypt password
        if (cache.getPassword() != null && !cache.getPassword().isEmpty()) {
            cache.setPassword(CryptoUtil.encrypt(cache.getPassword()));
        }

        try {

            if (!Files.exists(filePath)) {
                Files.createDirectories(filePath.getParent());
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("securityMode", cache.getSecurityMode().name());
            data.put("username", cache.getUsername());
            data.put("password", cache.getPassword());

            try (Writer writer = Files.newBufferedWriter(filePath)) {
                yaml.dump(data, writer);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save cache", e);
        }
    }

    @Override
    public synchronized SettingCache load() {
        if (!Files.exists(filePath)) return new SettingCache();

        try (InputStream in = Files.newInputStream(filePath)) {
            Map<String, Object> data = yaml.load(in);
            if (data == null) return new SettingCache();

            SettingCache cache = new SettingCache();
            cache.setSecurityMode(
                    SecurityMode.valueOf((String) data.getOrDefault("securityMode", "NONE"))
            );
            cache.setUsername((String) data.get("username"));
            cache.setPassword((String) data.get("password"));

            return cache;
        } catch (IOException e) {
            return new SettingCache();
        }
    }

    @Override
    public SettingCache loadDecrypted() {
        SettingCache cache = load();
        if (cache.getPassword() != null && !cache.getPassword().isEmpty()) {
            cache.setPassword(CryptoUtil.decrypt(cache.getPassword()));
        }
        return cache;
    }
}
