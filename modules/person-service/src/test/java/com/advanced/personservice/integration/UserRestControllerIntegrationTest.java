package com.advanced.personservice.integration;

import com.advanced.contract.model.*;
import com.advanced.personservice.model.Country;
import com.advanced.personservice.repository.CountryRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class UserRestControllerIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private CountryRepository countryRepository;

    private static final Network network = Network.newNetwork();

    @Container
    private static final GenericContainer<?> postgresContainer = new GenericContainer<>("postgres:latest")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_USER", "person-service-test")
            .withEnv("POSTGRES_PASSWORD", "password")
            .withEnv("POSTGRES_DB", "db")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        Map<String, String> postgresEnvMap = postgresContainer.getEnvMap();

        String jdbcUrl = "jdbc:postgresql://" + postgresContainer.getHost()
                + ":" + postgresContainer.getFirstMappedPort()
                + "/" + postgresEnvMap.get("POSTGRES_DB");
        System.out.println("jdbcUrl: " + jdbcUrl);

        registry.add("spring.datasource.url", () -> jdbcUrl);
        registry.add("spring.datasource.username", () -> postgresEnvMap.get("POSTGRES_USER"));
        registry.add("spring.datasource.password", () -> postgresEnvMap.get("POSTGRES_PASSWORD"));
        registry.add("spring.flyway.url", () -> jdbcUrl);
        registry.add("spring.flyway.user", () -> postgresEnvMap.get("POSTGRES_USER"));
        registry.add("spring.flyway.password", () -> postgresEnvMap.get("POSTGRES_PASSWORD"));
    }

    @BeforeAll
    static void startContainers() {
        postgresContainer.start();
    }

    @AfterAll
    static void tearDown() {
        postgresContainer.stop();
    }

    @BeforeEach
    void initCountries() {
        countryRepository.deleteAll();
        Country c = new Country();
        c.setName("Russia");
        c.setAlpha2("RU");
        c.setAlpha3("RUS");
        c.setStatus("ACTIVE");
        countryRepository.saveAndFlush(c);
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        UserDto userDto = createUserDto();

        webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserDto.class)
                .consumeWith(response -> {
                    UserDto result = response.getResponseBody();
                    assertNotNull(result);
                    assertNotNull(result.getId());
                    assertEquals(userDto.getEmail(), result.getEmail());
                    assertEquals(userDto.getFirstName(), result.getFirstName());
                    assertEquals(userDto.getLastName(), result.getLastName());
                    assertNotNull(result.getAddress());
                    assertEquals(userDto.getAddress().getAddress(), result.getAddress().getAddress());
                    assertNotNull(result.getIndividual());
                    assertEquals(userDto.getIndividual().getPassportNumber(), result.getIndividual().getPassportNumber());
                });
    }

    @Test
    void createUser_shouldReturnBadRequest_whenEmailIsInvalid() {
        UserDto userDto = createUserDto();
        userDto.setEmail("invalid-email");

        webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ErrorValidationResponse.class)
                .consumeWith(response -> {
                    ErrorValidationResponse error = response.getResponseBody();
                    assertNotNull(error);
                    assertEquals(400, error.getStatus());
                    assertNotNull(error.getErrors());
                    assertFalse(error.getErrors().isEmpty());
                });
    }

    @Test
    void getUserById_shouldReturnUserDto_whenUserExists() {
        UserDto createdUser = createUserAndGetResponse();

        webTestClient.get()
                .uri("/api/v1/users/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .consumeWith(response -> {
                    UserDto result = response.getResponseBody();
                    assertNotNull(result);
                    assertEquals(createdUser.getId(), result.getId());
                    assertEquals(createdUser.getEmail(), result.getEmail());
                    assertEquals(createdUser.getAddress().getAddress(), result.getAddress().getAddress());
                    assertEquals(createdUser.getIndividual().getPassportNumber(), result.getIndividual().getPassportNumber());
                });
    }

    @Test
    void getUserById_shouldReturnNotFound_whenUserDoesNotExist() {
        webTestClient.get()
                .uri("/api/v1/users/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .consumeWith(response -> {
                    ErrorResponse error = response.getResponseBody();
                    assertNotNull(error);
                    assertEquals(404, error.getStatus());
                    assertTrue(error.getError().contains("User not found with id"));
                });
    }

    @Test
    void updateUserById_shouldUpdateUserSuccessfully() {
        UserDto createdUser = createUserAndGetResponse();
        UserDto updatedUserDto = createUserDto();
        updatedUserDto.setFirstName("Jane");
        updatedUserDto.setLastName("Smith");
        updatedUserDto.getAddress().setAddress("New St, 456");
        updatedUserDto.getIndividual().setPassportNumber("9876 543210");
        assertNotNull(createdUser.getAddress());
        updatedUserDto.getAddress().setId(createdUser.getAddress().getId());
        assertNotNull(createdUser.getIndividual());
        updatedUserDto.getIndividual().setId(createdUser.getIndividual().getId());

        webTestClient.put()
                .uri("/api/v1/users/{id}", createdUser.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updatedUserDto)
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserDto.class)
                .consumeWith(response -> {
                    UserDto result = response.getResponseBody();
                    assertNotNull(result);
                    assertEquals(createdUser.getId(), result.getId());
                    assertEquals(updatedUserDto.getFirstName(), result.getFirstName());
                    assertEquals(updatedUserDto.getLastName(), result.getLastName());
                    assertEquals(updatedUserDto.getAddress().getAddress(), result.getAddress().getAddress());
                    assertEquals(updatedUserDto.getIndividual().getPassportNumber(), result.getIndividual().getPassportNumber());
                });
    }

    @Test
    void updateUserById_shouldReturnNotFound_whenUserDoesNotExist() {
        UserDto userDto = createUserDto();

        webTestClient.put()
                .uri("/api/v1/users/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .consumeWith(response -> {
                    ErrorResponse error = response.getResponseBody();
                    assertNotNull(error);
                    assertEquals(404, error.getStatus());
                    assertTrue(error.getError().contains("User not found with id"));
                });
    }

    @Test
    void compensateCreateUser_shouldDeleteUserSuccessfully() {
        UserDto createdUser = createUserAndGetResponse();

        webTestClient.delete()
                .uri("/api/v1/users/{id}/compensate", createdUser.getId())
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/v1/users/{id}", createdUser.getId())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void compensateCreateUser_shouldReturnNotFound_whenUserDoesNotExist() {
        webTestClient.delete()
                .uri("/api/v1/users/{id}/compensate", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound()
                .expectBody(ErrorResponse.class)
                .consumeWith(response -> {
                    ErrorResponse error = response.getResponseBody();
                    assertNotNull(error);
                    assertEquals(404, error.getStatus());
                    assertNotNull(error.getError());
                    assertTrue(error.getError().contains("User not found with id"));
                });
    }

    private UserDto createUserAndGetResponse() {
        UserDto userDto = createUserDto();
        return webTestClient.post()
                .uri("/api/v1/users")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(userDto)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(UserDto.class)
                .returnResult()
                .getResponseBody();
    }

    private UserDto createUserDto() {
        CountryDto countryDto = new CountryDto();
        countryDto.setId(1);

        AddressDto addressDto = new AddressDto();
        addressDto.setCountry(countryDto);
        addressDto.setAddress("Lenin St, 123");
        addressDto.setZipCode("123456");
        addressDto.setCity("Moscow");
        addressDto.setState("Moscow Oblast");

        IndividualDto individualDto = new IndividualDto();
        individualDto.setPassportNumber("1234 567890");
        individualDto.setPhoneNumber("+79991234567");
        individualDto.setVerifiedAt(OffsetDateTime.now());
        individualDto.setStatus("VERIFIED");

        UserDto userDto = new UserDto();
        userDto.setSecretKey("secret123");
        userDto.setEmail("test@example.com");
        userDto.setFirstName("John");
        userDto.setLastName("Doe");
        userDto.setFilled(true);
        userDto.setAddress(addressDto);
        userDto.setIndividual(individualDto);

        return userDto;
    }


}
