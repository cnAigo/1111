package org.example.testvue.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String ALLURE_DIR = "file:" + System.getProperty("user.dir")
            + "/target/site/allure-maven-plugin/";

    /**
     * Serve Allure HTML report.
     * Allure's app.js bundles webpack publicPath="/", so AJAX calls go to /data/, /widgets/, etc.
     * We map both the explicit /allure-report/ prefix AND the root-level asset paths so the
     * report works regardless of whether it's accessed at /allure-report/index.html or via the
     * Vite dev-server proxy.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Main entry point
        registry.addResourceHandler("/allure-report/**")
                .addResourceLocations(ALLURE_DIR).setCachePeriod(0);

        // Root-level assets that Allure's JS fetches (publicPath is "/")
        registry.addResourceHandler(
                "/index.html",
                "/app.js", "/styles.css", "/favicon.ico",
                "/data/**", "/widgets/**", "/export/**", "/history/**"
        ).addResourceLocations(ALLURE_DIR).setCachePeriod(0);
    }

    /** SPA fallback: serve index.html for unmatched routes */
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> spaForward() {
        return factory -> factory.addErrorPages(
            new ErrorPage(HttpStatus.NOT_FOUND, "/index.html")
        );
    }
}
