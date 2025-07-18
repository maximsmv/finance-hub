package com.advanced.transactionservice.controller.transaction.init;

import com.advanced.contract.model.DepositInitRequest;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class InitDepositRestControllerV1Test {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRequestRepository requestRepository;

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
    void initDeposit_shouldCreatePaymentRequest() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(100));

        DepositInitRequest request = new DepositInitRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transactionUid").isNotEmpty();

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }

    @Test
    void initDeposit_shouldReturnBadRequest_whenAmountIsMissing() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(100));

        DepositInitRequest request = new DepositInitRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }

    @Test
    void initDeposit_shouldReturnBadRequest_whenAmountIsNegative() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(100));

        DepositInitRequest request = new DepositInitRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(-100));

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }

    @Test
    void initDeposit_shouldReturnBadRequest_whenWalletUidIsMissing() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(100));

        DepositInitRequest request = new DepositInitRequest();
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(100));

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }

    @Test
    void initDeposit_shouldReturnBadRequest_whenCurrencyIsMissing() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(100));

        DepositInitRequest request = new DepositInitRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }

    @Test
    void initDeposit_shouldReturnNotFound_whenWalletDoesNotExist() {
        DepositInitRequest request = new DepositInitRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(100));

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.NOT_FOUND);

        List<PaymentRequest> requests = requestRepository.findAll();
        assertTrue(requests.isEmpty());
    }


}
