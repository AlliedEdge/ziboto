package com.ziboto.backend.config;

import com.ziboto.backend.config.properties.AppProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsConfigTest {

    private CorsConfiguration configuration;

    @BeforeEach
    void setUp() {
        configuration = buildConfiguration(
                "https://ziboto.alliededge.app, http://localhost:5173");
    }

    @Test
    void productionOriginAllowsRegistrationPreflight() {
        assertEquals("https://ziboto.alliededge.app",
                configuration.checkOrigin("https://ziboto.alliededge.app"));
        assertTrue(configuration.getAllowedMethods().contains("POST"));
        assertTrue(configuration.checkHeaders(java.util.List.of("content-type")).contains("content-type"));
        assertTrue(configuration.getAllowedMethods().contains("OPTIONS"));
    }

    @Test
    void disallowedOriginIsRejected() {
        assertEquals(null, configuration.checkOrigin("https://attacker.example"));
        assertFalse(configuration.getAllowedOrigins().contains("*"));
    }

    @Test
    void prodProfileAlwaysIncludesProductionFrontendOrigin() {
        CorsConfiguration prodConfiguration = buildConfiguration(
                "http://localhost:5173", "prod");

        assertTrue(prodConfiguration.getAllowedOrigins().contains(CorsConfig.PRODUCTION_FRONTEND_ORIGIN));
        assertEquals(CorsConfig.PRODUCTION_FRONTEND_ORIGIN,
                prodConfiguration.checkOrigin(CorsConfig.PRODUCTION_FRONTEND_ORIGIN));
    }

    private CorsConfiguration buildConfiguration(String allowedOrigins, String... activeProfiles) {
        AppProperties properties = new AppProperties();
        properties.getSecurity().getCors().setAllowedOrigins(allowedOrigins);
        properties.getSecurity().getCors().setAllowedMethods("GET,POST,PUT,PATCH,DELETE,OPTIONS");
        properties.getSecurity().getCors().setAllowedHeaders("Content-Type,Authorization,X-Requested-With");
        properties.getSecurity().getCors().setAllowCredentials(true);
        properties.getSecurity().getCors().setMaxAge(3600L);

        MockEnvironment environment = new MockEnvironment();
        for (String profile : activeProfiles) {
            environment.addActiveProfile(profile);
        }

        CorsConfig corsConfig = new CorsConfig(properties, environment);
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/register");
        return corsConfig.corsConfigurationSource().getCorsConfiguration(request);
    }
}
