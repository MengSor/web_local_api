package org.mengsor.web_local_api.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author mengsor
 * @date 2025/12/21
 * Redirects root "/" to /page/home
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // Redirect root "/" to /page/home
        registry.addRedirectViewController("/", "/page/home");
        registry.addRedirectViewController("/page", "/page/home");
    }
}
