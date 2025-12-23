package org.mengsor.web_local_api.configuration.directoryconfig;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.io.File;

@Validated
@ConfigurationProperties(prefix = "skyvva")
public class AppProperties {

    private String home;

    public String getHome() {
        return home;
    }

    public void setHome(String home) {
        this.home = home;
    }

    public File getConfigDir() {
        return new File(home, "config");
    }

    public File getApiConfigFile() {
        return new File(getConfigDir(), "API_application.properties");
    }

    public File getApiConfigBackupFile() {
        return new File(getConfigDir(), "API_application.properties.old");
    }
}
