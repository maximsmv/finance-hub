package com.advanced.individualsapi.integration;

import com.advanced.contract.model.*;
import com.advanced.individualsapi.configuration.MockJwtDecoderConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.JsonBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.MockServerContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ActiveProfiles("test")
@Import(MockJwtDecoderConfig.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class WalletIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ObjectMapper mapper;

    @Container
    private static final MockServerContainer walletService = new MockServerContainer(
            DockerImageName.parse("mockserver/mockserver:5.15.0")
    );

    private static MockServerClient mockServerClient;

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("transaction-service.base-url", walletService::getEndpoint);
    }

    @BeforeAll
    static void setupMockServer() {
        mockServerClient = new MockServerClient(walletService.getHost(), walletService.getServerPort());
    }

    @AfterEach
    void resetMockServer() {
        mockServerClient.reset();
    }

    private final static UUID USER_UID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void getWalletByUid_shouldReturnExpectedWallet() throws JsonProcessingException {
        UUID walletUid = UUID.randomUUID();

        WalletResponse wallet = new WalletResponse()
                .walletUid(walletUid)
                .walletTypeUid(UUID.randomUUID())
                .userUid(USER_UID)
                .currency("RUB")
                .balance(BigDecimal.valueOf(500.00))
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now());

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/wallets/" + walletUid + "/" + USER_UID)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(wallet))
        );

        webTestClient.get()
                .uri("/api/v1/wallets/" + walletUid)
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.wallet_uid").isEqualTo(walletUid.toString())
                .jsonPath("$.currency").isEqualTo("RUB")
                .jsonPath("$.balance").isEqualTo(500.00);
    }

    @Test
    void createWallet_shouldReturnCreatedWallet() throws JsonProcessingException {
        UUID walletUid = UUID.randomUUID();
        UUID walletTypeUid = UUID.randomUUID();

        com.advanced.individualsapi.dto.CreateWalletRequest createWalletRequest =
                new com.advanced.individualsapi.dto.CreateWalletRequest("test-wallet", walletTypeUid);

        CreateWalletRequest requestWithUserUid = new CreateWalletRequest()
                .name("test-wallet")
                .walletTypeUid(walletTypeUid)
                .userUid(USER_UID);

        WalletResponse expectedWallet = new WalletResponse()
                .walletUid(walletUid)
                .walletTypeUid(walletTypeUid)
                .userUid(USER_UID)
                .currency("USD")
                .balance(BigDecimal.ZERO)
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now());

        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/wallets")
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonBody.json(mapper.writeValueAsString(requestWithUserUid)))
        ).respond(
                response()
                        .withStatusCode(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(expectedWallet))
        );

        webTestClient.post()
                .uri("/api/v1/wallets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(createWalletRequest))
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.wallet_uid").isEqualTo(walletUid.toString())
                .jsonPath("$.user_uid").isEqualTo(USER_UID.toString())
                .jsonPath("$.currency").isEqualTo("USD")
                .jsonPath("$.balance").isEqualTo(0.0)
                .jsonPath("$.status").isEqualTo("ACTIVE");
    }

    @Test
    void createWallet_shouldReturnBadRequest_whenRequestIsInvalid() throws JsonProcessingException {
        com.advanced.individualsapi.dto.CreateWalletRequest invalidRequest =
                new com.advanced.individualsapi.dto.CreateWalletRequest("test-wallet", null);

        webTestClient.post()
                .uri("/api/v1/wallets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(invalidRequest))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400);
    }

    @Test
    void getWalletsByCurrentUser_shouldReturnListOfWallets() throws JsonProcessingException {

        WalletResponse wallet1 = new WalletResponse()
                .walletUid(UUID.randomUUID())
                .walletTypeUid(UUID.randomUUID())
                .userUid(USER_UID)
                .currency("RUB")
                .balance(BigDecimal.valueOf(100.00))
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now());

        WalletResponse wallet2 = new WalletResponse()
                .walletUid(UUID.randomUUID())
                .walletTypeUid(UUID.randomUUID())
                .userUid(USER_UID)
                .currency("USD")
                .balance(BigDecimal.valueOf(250.00))
                .status("ACTIVE")
                .createdAt(OffsetDateTime.now());

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/wallets/user/" + USER_UID)
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(new WalletResponse[]{wallet1, wallet2}))
        );

        webTestClient.get()
                .uri("/api/v1/wallets")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].currency").isEqualTo("RUB")
                .jsonPath("$[1].currency").isEqualTo("USD");
    }

    @Test
    void searchTransactions_shouldReturnListOfTransactions() throws JsonProcessingException {
        UUID userUid = UUID.fromString("00000000-0000-0000-0000-000000000001");

        TransactionStatusResponse tx1 = new TransactionStatusResponse()
                .transactionUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .type("DEPOSIT")
                .amount(BigDecimal.valueOf(100.00))
                .status("PENDING");

        TransactionStatusResponse tx2 = new TransactionStatusResponse()
                .transactionUid(UUID.randomUUID())
                .walletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(200.00))
                .type("DEPOSIT")
                .status("COMPLETED");

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/transactions")
                        .withQueryStringParameter("userUid", userUid.toString())
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(new TransactionStatusResponse[]{tx1, tx2}))
        );

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("page", 0)
                        .queryParam("size", 10)
                        .build()
                )
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].status").isEqualTo("PENDING")
                .jsonPath("$[1].status").isEqualTo("COMPLETED");
    }

    @Test
    void initTransaction_shouldReturnTransactionInitResponse() throws JsonProcessingException {
        String type = "deposit";

        InitRequest initRequest = new InitRequest()
                .amount(BigDecimal.valueOf(100.00))
                .walletUid(UUID.randomUUID());

        TransactionInitResponse expectedResponse = new TransactionInitResponse()
                .amount(BigDecimal.valueOf(100.00))
                .fee(BigDecimal.valueOf(20.00))
                .totalAmount(BigDecimal.valueOf(120.00));

        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/transactions/" + type + "/init")
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonBody.json(mapper.writeValueAsString(initRequest)))
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(expectedResponse))
        );

        webTestClient.post()
                .uri("/api/v1/transactions/" + type + "/init")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(initRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.amount").isEqualTo(BigDecimal.valueOf(100.00))
                .jsonPath("$.fee").isEqualTo(BigDecimal.valueOf(20.00));
    }

    @Test
    void confirmTransaction_shouldReturnTransactionConfirmResponse() throws JsonProcessingException {
        String type = "deposit";
        UUID transactionUid = UUID.randomUUID();

        ConfirmRequest confirmRequest = new ConfirmRequest()
                .walletUid(UUID.randomUUID())
                .amount(BigDecimal.valueOf(100.00))
                .fee(BigDecimal.valueOf(20.00))
                .currency("RUB");

        TransactionConfirmResponse expectedResponse = new TransactionConfirmResponse()
                .transactionUid(transactionUid)
                .status(TransactionConfirmResponse.StatusEnum.PENDING);

        mockServerClient.when(
                request()
                        .withMethod("POST")
                        .withPath("/transactions/" + type + "/confirm")
                        .withHeader("Content-Type", "application/json")
                        .withBody(JsonBody.json(mapper.writeValueAsString(confirmRequest)))
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(expectedResponse))
        );

        webTestClient.post()
                .uri("/api/v1/transactions/" + type + "/confirm")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(mapper.writeValueAsString(confirmRequest))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transaction_uid").isEqualTo(transactionUid)
                .jsonPath("$.status").isEqualTo("pending");
    }

    @Test
    void getTransactionStatus_shouldReturnStatusResponse() throws JsonProcessingException {
        UUID transactionUid = UUID.randomUUID();

        TransactionStatusResponse expectedResponse = new TransactionStatusResponse()
                .transactionUid(transactionUid)
                .walletUid(UUID.randomUUID())
                .type("WITHDRAWAL")
                .amount(BigDecimal.valueOf(300.00))
                .status("COMPLETED");

        mockServerClient.when(
                request()
                        .withMethod("GET")
                        .withPath("/transactions/" + transactionUid + "/status")
        ).respond(
                response()
                        .withStatusCode(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(mapper.writeValueAsString(expectedResponse))
        );

        webTestClient.get()
                .uri("/api/v1/transactions/" + transactionUid + "/status")
                .header(HttpHeaders.AUTHORIZATION, "Bearer dummy-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transaction_uid").isEqualTo(transactionUid)
                .jsonPath("$.status").isEqualTo("COMPLETED");
    }
}


