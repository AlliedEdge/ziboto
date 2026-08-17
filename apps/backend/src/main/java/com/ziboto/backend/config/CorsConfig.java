package com.ziboto.backend.config;

import com.ziboto.backend.config.properties.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    static final String PRODUCTION_FRONTEND_ORIGIN = "https://ziboto.alliededge.app";

    private final AppProperties appProperties;
    private final Environment environment;
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        AppProperties.Security.Cors corsProps = appProperties.getSecurity().getCors();
        
        configuration.setAllowedOrigins(resolveAllowedOrigins(corsProps.getAllowedOrigins()));
        configuration.setAllowedMethods(splitAndTrim(corsProps.getAllowedMethods()));
        configuration.setAllowedHeaders(splitAndTrim(corsProps.getAllowedHeaders()));
        configuration.setAllowCredentials(corsProps.getAllowCredentials());
        configuration.setMaxAge(corsProps.getMaxAge());
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }

    List<String> resolveAllowedOrigins(String configuredOrigins) {
        List<String> origins = new ArrayList<>(splitAndTrim(configuredOrigins));

        if (Arrays.asList(environment.getActiveProfiles()).contains("prod")
                && !origins.contains(PRODUCTION_FRONTEND_ORIGIN)) {
            origins.add(0, PRODUCTION_FRONTEND_ORIGIN);
        }

        return List.copyOf(origins);
    }

    private List<String> splitAndTrim(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }

        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(entry -> !entry.isEmpty())
                .toList();
    }
}
