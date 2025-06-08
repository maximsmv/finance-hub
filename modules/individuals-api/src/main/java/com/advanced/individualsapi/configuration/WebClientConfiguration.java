package com.advanced.individualsapi.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

    @Bean(name = "keycloakWebClient")
    public WebClient webClient(@Value("${keycloak.auth-server-url:}") String keycloakUrl) {
        return WebClient.builder().baseUrl(keycloakUrl).build();
    }

}
