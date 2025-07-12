package com.advanced.transactionservice.controller.transaction.confirm;

import com.advanced.contract.model.TransferConfirmRequest;
import com.advanced.transactionservice.model.TransferOperation;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.repository.TransferOperationRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.utils.WalletUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ConfirmTransferRestControllerV1Test {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private TransferOperationRepository transferOperationRepository;

    @MockBean
    private CalculationFeeService calculationFeeService;

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
    void confirmTransfer_shouldTransferMoneyBetweenWallets() {
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal transferAmount = new BigDecimal("200.00");

        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "source", initialBalance);
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "target", BigDecimal.ZERO);

        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setTransactionUid(UUID.randomUUID());
        request.setWalletUid(fromWallet.getUid());
        request.setTargetWalletUid(toWallet.getUid());
        request.setAmount(transferAmount);
        request.setCurrency("RUB");
        request.setComment("Test transfer");

        when(calculationFeeService.calculationTransferFee()).thenReturn(new BigDecimal("0.00"));

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Wallet updatedFrom = walletRepository.findById(fromWallet.getUid()).orElseThrow();
        Wallet updatedTo = walletRepository.findById(toWallet.getUid()).orElseThrow();

        assertEquals(initialBalance.subtract(transferAmount), updatedFrom.getBalance());
        assertEquals(transferAmount, updatedTo.getBalance());

        assertEquals(2, paymentRequestRepository.count());
        assertNotNull(transferOperationRepository.findByTransactionUid((request.getTransactionUid())));
    }

}
