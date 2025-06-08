package com.advanced.individualsapi.configuration;

import com.advanced.individualsapi.controller.AuthRestControllerV1;
import com.advanced.individualsapi.controller.GlobalExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.web.reactive.server.WebTestClient;

@Configuration
public class WebTestClientConfiguration {

    @Bean
    public WebTestClient getAuthControllerWebClient(AuthRestControllerV1 authRestControllerV1) {
        return WebTestClient.bindToController(authRestControllerV1)
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

}
