package com.ziboto.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

/**
 * Elasticsearch configuration.
 * Configures connection to Elasticsearch cluster.
 * 
 * @author Ziboto Team
 * @since V2
 */
@Configuration
@ConditionalOnProperty(name = "app.elasticsearch.enabled", havingValue = "true", matchIfMissing = false)
@EnableElasticsearchRepositories(basePackages = "com.ziboto.backend.search.repository")
public class ElasticsearchConfig extends ElasticsearchConfiguration {
    
    @Value("${spring.elasticsearch.uris:localhost:9200}")
    private String elasticsearchUris;
    
    @Value("${spring.elasticsearch.username:}")
    private String username;
    
    @Value("${spring.elasticsearch.password:}")
    private String password;
    
    @Value("${spring.elasticsearch.connection-timeout:10s}")
    private String connectionTimeout;
    
    @Value("${spring.elasticsearch.socket-timeout:30s}")
    private String socketTimeout;
    
    @Override
    public ClientConfiguration clientConfiguration() {
        // Parse URIs and remove protocol prefix if present
        String[] hosts = elasticsearchUris.split(",");
        for (int i = 0; i < hosts.length; i++) {
            // Remove http:// or https:// prefix if present
            hosts[i] = hosts[i].replaceAll("^https?://", "");
        }
        
        // Build configuration step by step
        var builder = ClientConfiguration.builder()
                .connectedTo(hosts);
        
        // Add basic auth if credentials are provided
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return builder.withBasicAuth(username, password).build();
        }
        
        return builder.build();
    }
}
