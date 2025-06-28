package com.advanced.individualsapi.integration;

import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.InvalidAccessTokenException;
import com.advanced.individualsapi.exception.InvalidCredentialsException;
import com.advanced.individualsapi.exception.InvalidRefreshTokenException;
import com.advanced.individualsapi.exception.UserAlreadyExistsException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    public Mono<AuthResponse> register(RegistrationRequest request) {
        return getAdminAccessToken()
                .flatMap(adminToken -> checkUserExists(adminToken, request.user().getEmail())
                        .flatMap(isEmpty -> {
                            if (isEmpty) {
                                return Mono.error(new UserAlreadyExistsException());
                            }
                            return createUser(adminToken, request)
                                    .then(login(new LoginRequest(request.user().getEmail(), request.password())));
                        }));
    }

    @WithSpan
    protected Mono<Boolean> checkUserExists(String adminToken, String email) {
        return webClient.get()
                .uri(adminEndpoint + "/users?email=" + email)
                .header("Authorization", "Bearer " + adminToken)
                .retrieve()
                .bodyToMono(UserRepresentation[].class)
                .map(users -> users.length != 0);
    }

    @WithSpan
    private Mono<String> createUser(String adminToken, RegistrationRequest request) {
        UserRepresentation user = getUserRepresentation(request);
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
    //TODO: Кэшировать токен по expired лайфу
    protected Mono<String> getAdminAccessToken() {
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
                .map(KeycloakTokenResponse::access_token);
    }

    private static UserRepresentation getUserRepresentation(RegistrationRequest request) {
        return new UserRepresentation(
                request.user().getEmail(),
                request.user().getEmail(),
                true,
                Collections.singletonList(
                        new UserRepresentation.CredentialRepresentation(
                                false,
                                "password",
                                request.password()
                        )
                )
        );
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

@JsonIgnoreProperties(ignoreUnknown = true)
record UserRepresentation(
        String username,
        String email,
        boolean enabled,
        List<CredentialRepresentation> credentials
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    record CredentialRepresentation(
            boolean temporary,
            String type,
            String value
    ) {}
}