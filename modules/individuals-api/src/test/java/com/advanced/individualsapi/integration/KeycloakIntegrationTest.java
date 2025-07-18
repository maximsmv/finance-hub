package com.advanced.individualsapi.integration;

import com.advanced.contract.api.UserRestControllerV1Api;
import com.advanced.contract.model.ErrorResponse;
import com.advanced.contract.model.UserDto;
import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class KeycloakIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private UserRestControllerV1Api userRestApi;

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer<?> postgresContainer = new GenericContainer<>("postgres:latest")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_USER", "keycloak")
            .withEnv("POSTGRES_PASSWORD", "password")
            .withEnv("POSTGRES_DB", "keycloak")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @Container
    private static final GenericContainer<?> keycloakContainer = new GenericContainer<>("quay.io/keycloak/keycloak:latest")
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_DB", "postgres")
            .withEnv("KC_DB_URL", "jdbc:postgresql://postgres:5432/keycloak")
            .withEnv("KC_DB_USERNAME", "keycloak")
            .withEnv("KC_DB_PASSWORD", "password")
            .withEnv("KC_HTTP_PORT", "8080")
            .withEnv("KC_HOSTNAME", "localhost")
            .withCommand("start-dev --import-realm")
            .withCopyFileToContainer(
                    MountableFile.forClasspathResource("my-realm-realm.json"),
                    "/opt/keycloak/data/import/my-realm-realm.json"
            )
            .withNetwork(network)
            .dependsOn(postgresContainer);

    private LoginRequest loginRequest;

    private UserResponse userResponse;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String keycloakUrl = "http://" + keycloakContainer.getHost() + ":" + keycloakContainer.getMappedPort(8080);
        System.out.println("Keycloak URL: " + keycloakUrl);
        registry.add("keycloak.auth-server-url", () -> keycloakUrl);
        registry.add("keycloak.credentials.secret", () -> "test-client-secret");
        registry.add("keycloak.admin.secret", () -> "test-client-secret");
    }

    @BeforeEach
    void setUp() {
        loginRequest = new LoginRequest("testuser@example.com", "TestPassword123");
        userResponse = new UserResponse(null, loginRequest.email(), List.of("USER"), null);
    }

    @BeforeAll
    static void startContainers() {
        keycloakContainer.start();
    }

    @AfterAll
    static void tearDown() {
        keycloakContainer.stop();
    }

    @Test
    void login_Success_ReturnsAuthResponse() {
        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .expectBody(AuthResponse.class)
                .value(authResponse -> {
                    assertNotNull(authResponse.accessToken());
                    assertNotNull(authResponse.refreshToken());
                    assertEquals("Bearer", authResponse.tokenType());
                    assertEquals(300, authResponse.expiresIn());
                });
    }

    @Test
    void login_InvalidCredentials_ThrowsInvalidCredentialsException() {
        LoginRequest request = new LoginRequest(loginRequest.email(), "WrongPassword");
        InvalidCredentialsException expectedException = new InvalidCredentialsException();

        webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertEquals(expectedException.getMessage(), errorResponse.getError());
                    assertEquals(expectedException.getStatus(), errorResponse.getStatus());
                });
    }

    @Test
    void register_Success_ReturnsAuthResponseAndCreatesUser() {
        UserDto user = new UserDto();
        user.setId(UUID.randomUUID());
        user.setEmail("test@example.com");
        RegistrationRequest registrationRequest = new RegistrationRequest(user, "password", "password");

        when(userRestApi.createUser(any(UserDto.class))).thenReturn(Mono.just(user));

        webTestClient.post()
                .uri("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationRequest)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .value(authResponse -> {
                    assertNotNull(authResponse.accessToken());
                    assertNotNull(authResponse.refreshToken());
                    assertEquals("Bearer", authResponse.tokenType());
                    assertEquals(300, authResponse.expiresIn());
                });
    }

    @Test
    void register_ExistingUser_ThrowsUserAlreadyExistsException() {
        UserDto user = new UserDto();
        user.setEmail(loginRequest.email());
        user.setId(UUID.randomUUID());
        RegistrationRequest request = new RegistrationRequest(user, loginRequest.password(), loginRequest.password());
        when(userRestApi.createUser(any(UserDto.class))).thenReturn(Mono.just(user));
        when(userRestApi.compensateCreateUser(any())).thenReturn(Mono.empty());

        UserAlreadyExistsException expectedException = new UserAlreadyExistsException();
        webTestClient.post()
                .uri("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT)
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertEquals(expectedException.getMessage(), errorResponse.getError());
                    assertEquals(expectedException.getStatus(), errorResponse.getStatus());
                });
    }

    @Test
    void register_PasswordMismatch_ReturnsBadRequest() {
        UserDto user = new UserDto();
        user.setEmail("wrongtest@example.com");
        RegistrationRequest registrationPasswordMismatchRequest = new RegistrationRequest(user, "password", "wrongpassword");
        PasswordMismatchException expectedException = new PasswordMismatchException();
        webTestClient.post()
                .uri("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(registrationPasswordMismatchRequest)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertEquals(expectedException.getMessage(), errorResponse.getError());
                    assertEquals(expectedException.getStatus(), errorResponse.getStatus());
                });
    }

    @Test
    void refreshToken_Success_ReturnsNewAuthResponse() {
        Mono<AuthResponse> authResponseMono = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(AuthResponse.class)
                .getResponseBody()
                .single();

        Mono<AuthResponse> refreshedResponseMono = authResponseMono.flatMap(authResponse -> {
            RefreshTokenRequest refreshTokenRequest = new RefreshTokenRequest(authResponse.refreshToken());
            return webTestClient.post()
                    .uri("/api/v1/auth/refresh-token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(refreshTokenRequest)
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(AuthResponse.class)
                    .getResponseBody()
                    .single();
        });

        StepVerifier.create(refreshedResponseMono)
                .assertNext(refreshedResponse -> {
                    assertNotNull(refreshedResponse.accessToken());
                    assertNotNull(refreshedResponse.refreshToken());
                })
                .verifyComplete();
    }

    @Test
    void refreshToken_InvalidToken_ThrowsInvalidTokenException() {
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");
        InvalidRefreshTokenException expectedException = new InvalidRefreshTokenException();

        webTestClient.post()
                .uri("/api/v1/auth/refresh-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertEquals(expectedException.getMessage(), errorResponse.getError());
                    assertEquals(expectedException.getStatus(), errorResponse.getStatus());
                });
    }

    @Test
    void getUserInfo_Success_ReturnsUserResponse() {
        Mono<AuthResponse> authResponseMono = webTestClient.post()
                .uri("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(AuthResponse.class)
                .getResponseBody()
                .single();

        Mono<UserResponse> userInfoResponseMono = authResponseMono.flatMap(authResponse -> webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", authResponse.tokenType() + " " + authResponse.accessToken())
                .exchange()
                .expectStatus().isOk()
                .returnResult(UserResponse.class)
                .getResponseBody()
                .single());

        StepVerifier.create(userInfoResponseMono)
                .assertNext(userInfoResponse -> {
                    assertNotNull(userInfoResponse.id());
                    assertNotNull(userInfoResponse.createdAt());
                    assertEquals(userResponse.email(), userInfoResponse.email());
                    assertEquals(userResponse.roles(), userInfoResponse.roles());
                })
                .verifyComplete();
    }

    @Test
    void getUserInfo_InvalidAccessToken_ThrowsInvalidAccessTokenException() {
        String invalidAccessToken = "Bearer invalid-access-token";
        InvalidAccessTokenException expectedException = new InvalidAccessTokenException();
        webTestClient.get()
                .uri("/api/v1/auth/me")
                .header("Authorization", invalidAccessToken)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody(ErrorResponse.class)
                .value(errorResponse -> {
                    assertEquals(expectedException.getMessage(), errorResponse.getError());
                    assertEquals(expectedException.getStatus(), errorResponse.getStatus());
                });
    }
}