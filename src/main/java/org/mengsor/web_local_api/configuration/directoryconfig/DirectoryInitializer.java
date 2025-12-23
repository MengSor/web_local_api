package org.mengsor.web_local_api.configuration.directoryconfig;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

@Slf4j
@Configuration
public class DirectoryInitializer {

    private static final String DEFAULT_FILE_NAME = "application.properties";
    private static final String USER_FILE_NAME = "API_application.properties";
    private static final String USER_BACKUP_NAME = "API_application.properties.old";

    private final String skyvvaHome;

    public DirectoryInitializer() {
        this.skyvvaHome = System.getenv().getOrDefault("SKYVVA_HOME",
                System.getProperty("user.home") + "/.skyvva");
    }

    @PostConstruct
    public void init() throws IOException {

        // Create directories
        File configDir = new File(skyvvaHome, "config");
        File logsDir = new File(skyvvaHome, "logs");
        File cacheDir = new File(skyvvaHome, "cache");

        createDir(configDir);
        createDir(logsDir);
        createDir(cacheDir);

        File userConfig = new File(configDir, USER_FILE_NAME);
        File userBackup = new File(configDir, USER_BACKUP_NAME);

        // Create API_application.properties from default if missing
        if (!userConfig.exists()) {
            try (var in = Objects.requireNonNull(
                    getClass().getClassLoader().getResourceAsStream(DEFAULT_FILE_NAME),
                    "Default application.properties not found in JAR")) {
                Files.copy(in, userConfig.toPath());
                System.out.println(" Created default config: " + userConfig.getAbsolutePath());
            }
        }

        // Backup current config to .old on every run
        if (userConfig.exists()) {
            Files.copy(userConfig.toPath(),
                    userBackup.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            System.out.println(" Backup saved: " + userBackup.getAbsolutePath());
        }

        // Tell Spring Boot to use the external config
        log.info("Setting Spring Boot external config location to: {}", userConfig.getAbsolutePath());
        System.setProperty("spring.config.additional-location", "file:" + userConfig.getAbsolutePath());
        System.out.println(" Spring Boot external config location set: " + userConfig.getAbsolutePath());
    }

    private void createDir(File dir) {
        if (!dir.exists() && !dir.mkdirs()) {
            log.error("Cannot create directory: {}", dir);
            throw new IllegalStateException("Cannot create directory: " + dir);
        }
    }
}
