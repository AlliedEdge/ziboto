package com.ziboto.backend.config;

import org.springframework.context.annotation.Configuration;

/**
 * Web configuration for the application.
 * Spring Boot 4 auto-configures Jackson 3 (JsonMapper) by default.
 * Additional customization can be done through application.yml properties.
 */
@Configuration
public class WebConfig {
    // Jackson 3 (JsonMapper) is auto-configured by Spring Boot 4
    // Configure through application.yml: spring.jackson.*
}
