package com.advanced.individualsapi.service.util;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class JwtUtilService {

    public Mono<UUID> extractUserUid(Jwt jwt) {
        Object userUidRaw = jwt.getClaims().get("user_id");

        if (userUidRaw == null) {
            return Mono.error(new IllegalArgumentException("user_id not found in token"));
        }

        try {
            return Mono.just(UUID.fromString(userUidRaw.toString()));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new IllegalArgumentException("Invalid user_id format in token"));
        }
    }
}
