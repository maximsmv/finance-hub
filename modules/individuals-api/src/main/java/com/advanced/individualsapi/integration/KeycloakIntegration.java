package com.advanced.individualsapi.integration;

import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.InvalidAccessTokenException;
import com.advanced.individualsapi.exception.InvalidCredentialsException;
import com.advanced.individualsapi.exception.InvalidRefreshTokenException;
import com.advanced.individualsapi.exception.UserAlreadyExistsException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.jspecify.annotations.NonNull;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.keycloak.representations.idm.UserRepresentation;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Component
public class KeycloakIntegration {

    private final WebClient webClient;

    private final String clientId;

    private final String clientSecret;

    private final String adminClientId;

    private final String adminClientSecret;

    private final String adminEndpoint;

    private final String tokenEndpoint;

    private final String userInfoEndpoint;

    private static final String ADMIN_TOKEN_KEY = "admin-token";

    private final Cache<String, CachedToken> tokenCache = Caffeine.newBuilder()
            .expireAfter(new Expiry<String, CachedToken>() {

                @Override
                public long expireAfterCreate(@NonNull String key, @NonNull CachedToken value, long currentTime) {
                    Duration ttl = Duration.between(Instant.now(), value.expiry());
                    return ttl.toNanos();
                }

                @Override
                public long expireAfterUpdate(@NonNull String key,@NonNull CachedToken value, long currentTime, long currentDuration) {
                    return currentDuration;
                }

                @Override
                public long expireAfterRead(@NonNull String key,@NonNull CachedToken value, long currentTime, long currentDuration) {
                    return currentDuration;
                }
            })
            .build();

    public KeycloakIntegration(
            @Qualifier("keycloakWebClient") WebClient webClient,
            @Value("${keycloak.resource:}") String clientId,
            @Value("${keycloak.credentials.secret:}") String clientSecret,
            @Value("${keycloak.admin.client-id}") String adminClientId,
            @Value("${keycloak.admin.secret}") String adminClientSecret,
            @Value("${keycloak.admin.endpoint}") String adminEndpoint,
            @Value("${keycloak.endpoints.token}") String tokenEndpoint,
            @Value("${keycloak.endpoints.userInfo}") String userInfoEndpoint
    ) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClient = webClient;
        this.adminClientId = adminClientId;
        this.adminClientSecret = adminClientSecret;
        this.adminEndpoint = adminEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
    }

    @WithSpan
    public Mono<AuthResponse> register(RegistrationRequest request, String userId) {
        return getAdminAccessToken()
                .flatMap(adminToken ->
                        createUser(adminToken, request, userId)
                                .onErrorResume(WebClientResponseException.class, ex -> {
                                    if (ex.getStatusCode() == HttpStatus.CONFLICT || ex.getStatusCode() == HttpStatus.BAD_REQUEST) {
                                        return Mono.error(new UserAlreadyExistsException());
                                    }
                                    return Mono.error(new RuntimeException("Keycloak error: " + ex.getMessage()));
                                })
                                .then(login(new LoginRequest(request.user().getEmail(), request.password())))
                );
    }

    @WithSpan
    private Mono<String> createUser(String adminToken, RegistrationRequest request, String userId) {
        UserRepresentation user = getUserRepresentation(request, userId);
        return webClient.post()
                .uri(adminEndpoint + "/users")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(user)
                .retrieve()
                .toBodilessEntity()
                .map(response -> Objects.requireNonNull(response.getHeaders().getLocation())
                        .getPath().replaceAll(".*/([^/]+)$", "$1"));
    }

    @WithSpan
    public Mono<AuthResponse> login(LoginRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "password");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("username", request.email());
        formData.add("password", request.password());
        formData.add("scope", "openid");

        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new InvalidCredentialsException())))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Keycloak server error: " + error))))
                .bodyToMono(KeycloakTokenResponse.class)
                .map(token -> new AuthResponse(
                        token.access_token(),
                        token.expires_in(),
                        token.refresh_token(),
                        token.token_type()
                ));
    }

    @WithSpan
    public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", request.refreshToken());
        formData.add("scope", "openid");

        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new InvalidRefreshTokenException())))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Keycloak server error: " + error))))
                .bodyToMono(KeycloakTokenResponse.class)
                .map(token -> new AuthResponse(
                        token.access_token(),
                        token.expires_in(),
                        token.refresh_token(),
                        token.token_type()
                ));
    }

    @WithSpan
    public Mono<UserResponse> getUserInfo(String accessToken) {
        return webClient.get()
                .uri(userInfoEndpoint)
                .header("Authorization", accessToken)
                .exchangeToMono(response -> {
                    if (response.statusCode().is4xxClientError()) {
                        return response.bodyToMono(String.class)
                                .switchIfEmpty(Mono.just("Invalid token"))
                                .flatMap(error -> Mono.error(new InvalidAccessTokenException()));
                    }
                    if (response.statusCode().is5xxServerError()) {
                        return response.bodyToMono(String.class)
                                .switchIfEmpty(Mono.just("Server error"))
                                .flatMap(error -> Mono.error(new RuntimeException("Keycloak server error: " + error)));
                    }
                    return response.bodyToMono(KeycloakUserInfo.class)
                            .map(userInfo -> new UserResponse(
                                    userInfo.sub(),
                                    userInfo.email(),
                                    userInfo.roles(),
                                    userInfo.created_at()
                            ));
                });
    }

    private Mono<String> getAdminAccessToken() {
        CachedToken cached = tokenCache.getIfPresent(ADMIN_TOKEN_KEY);
        if (Objects.nonNull(cached) && Instant.now().isBefore(cached.expiry())) {
            return Mono.just(cached.token());
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", adminClientId);
        formData.add("client_secret", adminClientSecret);

        return webClient.post()
                .uri(tokenEndpoint)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Failed to get admin token: " + error))))
                .onStatus(HttpStatusCode::is5xxServerError, response -> response.bodyToMono(String.class)
                        .flatMap(error -> Mono.error(new RuntimeException("Keycloak server error: " + error))))
                .bodyToMono(KeycloakTokenResponse.class)
                .map(tokenResponse -> {
                    Instant expiry = Instant.now().plusSeconds(tokenResponse.expires_in());
                    tokenCache.put(ADMIN_TOKEN_KEY, new CachedToken(tokenResponse.access_token(), expiry));
                    return tokenResponse.access_token();
                });
    }

    private static UserRepresentation getUserRepresentation(RegistrationRequest request, String userId) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setTemporary(false);
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(request.password());

        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("user_id", List.of(userId));

        UserRepresentation user = new UserRepresentation();
        user.setEmail(request.user().getEmail());
        user.setUsername(request.user().getEmail());
        user.setEnabled(true);
        user.setCredentials(Collections.singletonList(credential));
        user.setAttributes(attributes);

        return user;
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
record KeycloakTokenResponse(
        String access_token,
        long expires_in,
        String refresh_token,
        String token_type
) {}

@JsonIgnoreProperties(ignoreUnknown = true)
record KeycloakUserInfo(
        String sub,
        String email,
        List<String> roles,
        String created_at
) {}

//@JsonIgnoreProperties(ignoreUnknown = true)
//record UserRepresentation(
//        String username,
//        String email,
//        boolean enabled,
//        List<CredentialRepresentation> credentials,
//        Map<String, List<String>> attributes
//) {
//    @JsonIgnoreProperties(ignoreUnknown = true)
//    record CredentialRepresentation(
//            boolean temporary,
//            String type,
//            String value
//    ) {}
//}

record CachedToken(
        String token, Instant expiry
) {}