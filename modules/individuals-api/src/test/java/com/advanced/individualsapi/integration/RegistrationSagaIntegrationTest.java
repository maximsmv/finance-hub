package com.advanced.individualsapi.integration;


import com.advanced.contract.model.ErrorResponse;
import com.advanced.contract.model.UserDto;
import com.advanced.individualsapi.dto.AuthResponse;
import com.advanced.individualsapi.dto.RegistrationRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class RegistrationSagaIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private KeycloakIntegration keycloakIntegration;

    @Container
    private static final MockServerContainer mockPersonService = new MockServerContainer(DockerImageName.parse("mockserver/mockserver").withTag("5.15.0"));

    private static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void overridePersonServiceUrl(DynamicPropertyRegistry registry) {
        mockServerClient = new MockServerClient(
                mockPersonService.getHost(),
                mockPersonService.getServerPort()
        );
        registry.add("person-service.base-url", mockPersonService::getEndpoint);
    }

    @BeforeEach
    void setUp() {
        mockServerClient.reset();
    }

    @AfterAll
    static void tearDown() {
        if (mockServerClient != null) {
            mockServerClient.stop();
        }
        mockPersonService.stop();
    }

    private RegistrationRequest createValidRequest(String email, UUID userId) {
        UserDto user = new UserDto();
        user.setId(userId);
        user.setEmail(email);
        user.setFilled(true);
        user.setFirstName("Test");
        user.setLastName("User");

        return new RegistrationRequest(user, "password", "password");
    }


    @Test
    void registration_success_whenPersonServiceAndKeycloakWork() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        RegistrationRequest request = createValidRequest("test@example.com", userId);

        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/users")
        ).respond(
                response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(request.user()))
        );

        AuthResponse authResponse = new AuthResponse("token", 300, "refresh", "Bearer");
        when(keycloakIntegration.register(any())).thenReturn(Mono.just(authResponse));

        webTestClient.post()
                .uri("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(AuthResponse.class)
                .value(resp -> {
                    assertEquals("Bearer", resp.tokenType());
                    assertEquals("token", resp.accessToken());
                });
    }

    @Test
    void registration_compensates_if_keycloak_fails() throws JsonProcessingException {
        UUID userId = UUID.randomUUID();
        RegistrationRequest request = createValidRequest("fail@example.com", userId);

        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/api/v1/users")
        ).respond(
                response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(new ObjectMapper().writeValueAsString(request.user()))
        );

        mockServerClient.when(
                request()
                        .withMethod("DELETE")
                        .withPath("/api/v1/users/" + userId + "/compensate")
        ).respond(
                response().withStatusCode(200)
        );

        Mockito.when(keycloakIntegration.register(any()))
                .thenReturn(Mono.error(new RuntimeException("Keycloak error")));

        webTestClient.post()
                .uri("/api/v1/auth/registration")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody(ErrorResponse.class)
                .value(resp -> {
                    assertNotNull(resp.getError());
                    assertTrue(resp.getError().toLowerCase().contains("keycloak"));
                });
    }
}
