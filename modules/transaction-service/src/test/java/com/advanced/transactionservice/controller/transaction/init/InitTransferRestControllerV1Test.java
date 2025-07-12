package com.advanced.transactionservice.controller.transaction.init;

import com.advanced.contract.model.TransferConfirmRequest;
import com.advanced.contract.model.TransferInitRequest;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class InitTransferRestControllerV1Test {

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
    void transfer_shouldSucceed_whenValidRequest() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from-wallet", BigDecimal.valueOf(500));
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to-wallet", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(300));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.transactionUid").isNotEmpty();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenFromWalletUidIsMissing() {
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenToWalletUidIsMissing() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenAmountIsMissing() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(100));
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenCurrencyIsMissing() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(100));
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenNotEnoughBalance() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(10));
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenFromWalletIsBlocked() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(500), WalletStatus.BLOCKED);
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenToWalletIsBlocked() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(500));
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100), WalletStatus.BLOCKED);

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnBadRequest_whenWalletsAreSame() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "same", BigDecimal.valueOf(1000));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(wallet.getUid());
        request.setToWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void initTransfer_shouldReturnNotFound_whenFromWalletNotFound() {
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(UUID.randomUUID());
        request.setToWalletUid(toWallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void initTransfer_shouldReturnNotFound_whenToWalletNotFound() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(100));

        TransferInitRequest request = new TransferInitRequest();
        request.setFromWalletUid(fromWallet.getUid());
        request.setToWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/init")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }
}
