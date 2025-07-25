package com.advanced.individualsapi.configuration;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@TestConfiguration
public class MockJwtDecoderConfig {

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return token -> {
            Map<String, Object> claims = new HashMap<>();
            claims.put("sub", "test-subject");
            claims.put("user_id", "00000000-0000-0000-0000-000000000001");

            Jwt jwt = new Jwt(
                    token,
                    Instant.now(),
                    Instant.now().plus(Duration.ofHours(1)),
                    Map.of("alg", "none"),
                    claims
            );

            return Mono.just(jwt);
        };
    }

}
