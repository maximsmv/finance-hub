package com.advanced.transactionservice.controller;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class WalletRestControllerV1Test {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    private static final Network network = Network.newNetwork();

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
            .withExposedPorts(5432)
            .withDatabaseName("testdb")
            .withUsername("user")
            .withPassword("pass")
            .withNetwork(network)
            .withNetworkAliases("postgres");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @BeforeAll
    static void startContainers() {
        postgres.start();
    }

    @AfterAll
    static void tearDown() {
        postgres.stop();
    }

    @BeforeEach
    void setup() {
        walletRepository.deleteAll();
    }

    @Test
    void createWallet_shouldCreateSuccessfully() {
        WalletType type = walletTypeRepository.save(createWalletType());

        UUID userUid = UUID.randomUUID();

        var request = new CreateWalletRequest();
        request.setName("Test Wallet");
        request.setUserUid(userUid);
        request.setWalletTypeUid(type.getUid());

        webTestClient.post()
                .uri("/api/v1/wallets")
                .bodyValue(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(WalletResponse.class)
                .value(response -> {
                    assertNotNull(response.getWalletUid());
                    assertEquals(WalletStatus.ACTIVE.getValue(), response.getStatus());
                    assertEquals(userUid, response.getUserUid());
                    assertEquals(BigDecimal.ZERO, response.getBalance());
                });
    }

    @Test
    void createWallet_shouldReturnBadRequest_whenWalletTypeNotFound() {
        var request = new CreateWalletRequest();
        request.setName("Broken Wallet");
        request.setUserUid(UUID.randomUUID());
        request.setWalletTypeUid(UUID.randomUUID());

        webTestClient.post()
                .uri("/api/v1/wallets")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.error").value(error -> assertTrue(((String) error).contains("Wallet type not found")));
    }

    @Test
    void createWallet_shouldReturnBadRequest_whenRequiredFieldsMissing() {
        var request = new CreateWalletRequest();
        request.setName("Invalid");

        webTestClient.post()
                .uri("/api/v1/wallets")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.errors").isArray();
    }


    @Test
    void getWalletByUid_shouldReturnCorrectWallet() {
        WalletType type = walletTypeRepository.save(createWalletType());
        Wallet wallet = createWallet("Test", type, new BigDecimal("100.00"), UUID.randomUUID());

        webTestClient.get()
                .uri("/api/v1/wallets/" + wallet.getUid())
                .exchange()
                .expectStatus().isOk()
                .expectBody(WalletResponse.class)
                .value(response -> {
                    assertEquals(wallet.getUid(), response.getWalletUid());
                    assertEquals(new BigDecimal("100.00"), response.getBalance());
                    assertEquals(WalletStatus.ACTIVE.getValue(), response.getStatus());
                });
    }

    @Test
    void getWalletByUid_shouldReturnNotFound_whenWalletDoesNotExist() {
        UUID walletUid = UUID.randomUUID();

        webTestClient.get()
                .uri("/api/v1/wallets/" + walletUid)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.error").exists();
    }

    @Test
    void getWalletByUid_shouldReturnBadRequest_whenUidInvalid() {
        webTestClient.get()
                .uri("/api/v1/wallets/not-a-uuid")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void getWalletsByUser_shouldReturnUserWallets() {
        WalletType type = walletTypeRepository.save(createWalletType());
        UUID userUid = UUID.randomUUID();

        createWallet("W1", type, new BigDecimal("10.00"), userUid);
        createWallet("W2", type, new BigDecimal("20.00"), userUid);

        webTestClient.get()
                .uri("/api/v1/wallets/user/" + userUid)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(WalletResponse.class)
                .value(wallets -> {
                    assertEquals(2, wallets.size());
                    assertTrue(wallets.stream().allMatch(w -> Objects.equals(w.getUserUid(), userUid)));
                });
    }

    private Wallet createWallet(String name, WalletType type, BigDecimal balance, UUID userUid) {
        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setWalletType(type);
        wallet.setUserUid(userUid);
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
        return wallet;
    }

    private WalletType createWalletType() {
        WalletType type = new WalletType();
        type.setCreator("system");
        type.setName("default");
        type.setUserType("default");
        type.setStatus(WalletStatus.ACTIVE);
        type.setCurrencyCode(Currency.getInstance("RUB"));
        walletTypeRepository.save(type);
        return type;
    }

}