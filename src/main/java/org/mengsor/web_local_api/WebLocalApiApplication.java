package org.mengsor.web_local_api;

import org.mengsor.web_local_api.configuration.directoryconfig.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import java.io.File;
import java.util.Map;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class WebLocalApiApplication {

	public static void main(String[] args) {
		//SpringApplication.run(WebLocalApiApplication.class, args);
		String skyvvaHome = System.getenv().getOrDefault("SKYVVA_HOME",
				System.getProperty("user.home") + "/.skyvva");
		File userConfig = new File(skyvvaHome + "/config/API_application.properties");

		SpringApplication app = new SpringApplication(WebLocalApiApplication.class);

		// Tell Spring Boot to load external config BEFORE starting
		if (userConfig.exists()) {
			app.setDefaultProperties(Map.of(
					"spring.config.location", "file:" + userConfig.getAbsolutePath()
			));
		}

		app.run(args);
	}

}
