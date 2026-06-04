package org.example.testvue.config;

import org.springframework.boot.web.server.ErrorPage;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;

@Configuration
public class WebConfig {
    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> spaForward() {
        return factory -> factory.addErrorPages(
            new ErrorPage(HttpStatus.NOT_FOUND, "/index.html")
        );
    }
}
