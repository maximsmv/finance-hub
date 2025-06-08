package com.advanced.individualsapi.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${keycloak.auth-server-url}${keycloak.endpoints.jwks}")
    private String jwkSetUri;

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        http
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/v1/auth/registration", "/v1/auth/login", "/v1/auth/refresh-token").permitAll()
                        .pathMatchers("/v1/auth/me").authenticated()
                        .anyExchange().permitAll()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtDecoder(reactiveJwtDecoder()))
                        .authenticationEntryPoint(new CustomAuthenticationEntryPoint())
                )
                .csrf(ServerHttpSecurity.CsrfSpec::disable);
        return http.build();
    }

    @Bean
    public ReactiveJwtDecoder reactiveJwtDecoder() {
        return NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build();
    }
}


