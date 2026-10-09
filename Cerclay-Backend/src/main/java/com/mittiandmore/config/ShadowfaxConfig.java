package com.mittiandmore.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(ShadowfaxProperties.class)
public class ShadowfaxConfig {

    @Bean
    public RestClient shadowfaxRestClient(ShadowfaxProperties properties) {
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .defaultHeader("Authorization", "Token " + properties.getApiToken())
            .defaultHeader("Content-Type", "application/json")
            .build();
    }
}
