package com.advanced.transactionservice.integration;


import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.exception.TransferSameWalletsException;
import com.advanced.transactionservice.exception.WalletBalanceException;
import com.advanced.transactionservice.exception.WalletStatusException;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.service.WalletService;
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
import java.math.RoundingMode;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class WalletServiceImplIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

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
    void createWallet_shouldPersistAndReturnCorrectData() {
        WalletType type = createWalletType();

        CreateWalletRequest request = new CreateWalletRequest();
        request.setUserUid(UUID.randomUUID());
        request.setWalletTypeUid(type.getUid());
        request.setName("123124121");

        WalletResponse response = walletService.createWallet(request);
        createWallet result = new createWallet(request, response);

        assertNotNull(result.response().getWalletUid());

        Wallet entity = walletRepository.findById(result.response().getWalletUid())
                .orElseThrow();

        assertEquals(result.request().getUserUid(), entity.getUserUid());
        assertEquals(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_EVEN), entity.getBalance());
        assertEquals(result.request().getName(), entity.getName());
    }

    private record createWallet(CreateWalletRequest request, WalletResponse response) {
    }

    @Test
    void transfer_shouldSucceed_whenDataIsValid() {
        WalletType type = walletTypeRepository.save(createWalletType());

        Wallet from = createWallet("Sender", type, new BigDecimal("100.00"));
        Wallet to = createWallet("Receiver", type, BigDecimal.ZERO);

        walletService.transfer(from.getUid(), to.getUid(), new BigDecimal("60.00"), new BigDecimal("60.00"));

        Wallet updatedFrom = walletRepository.findById(from.getUid()).orElseThrow();
        Wallet updatedTo = walletRepository.findById(to.getUid()).orElseThrow();

        assertEquals(new BigDecimal("40.00"), updatedFrom.getBalance());
        assertEquals(new BigDecimal("60.00"), updatedTo.getBalance());
    }

    @Test
    void transfer_shouldFail_whenSameWalletProvided() {
        WalletType type = createWalletType();
        Wallet wallet = createWallet("Same", type, new BigDecimal("50.00"));

        assertThrows(TransferSameWalletsException.class, () ->
                walletService.transfer(wallet.getUid(), wallet.getUid(), BigDecimal.TEN, BigDecimal.TEN)
        );
    }

    @Test
    void transfer_shouldFail_whenSenderWalletIsArchived() {
        WalletType type = createWalletType();
        Wallet from = createWallet("from", type, new BigDecimal("50.00"), WalletStatus.ARCHIVED);
        Wallet to = createWallet("to", type, new BigDecimal("50.00"));

        assertThrows(WalletStatusException.class, () ->
                walletService.transfer(from.getUid(), to.getUid(), BigDecimal.TEN, BigDecimal.TEN)
        );
    }

    @Test
    void transfer_shouldFail_whenSenderHasInsufficientBalance() {
        WalletType type = createWalletType();
        Wallet from = createWallet("from", type, new BigDecimal("50.00"));
        Wallet to = createWallet("to", type, new BigDecimal("50.00"));

        assertThrows(WalletBalanceException.class, () ->
                walletService.transfer(from.getUid(), to.getUid(), BigDecimal.valueOf(100), BigDecimal.valueOf(100))
        );
    }

    @Test
    void transfer_shouldMaintainBalanceIntegrity_underConcurrentAccess() throws InterruptedException {
        WalletType type = walletTypeRepository.save(createWalletType());

        Wallet from = walletRepository.save(createWallet("Sender", type, new BigDecimal("1000.00")));
        Wallet to = walletRepository.save(createWallet("Receiver", type, BigDecimal.ZERO));

        int threadCount = 100;
        BigDecimal amountPerTransfer = new BigDecimal("10.00");

        List<Exception> exceptions;
        try (ExecutorService executor = Executors.newFixedThreadPool(10)) {
            CountDownLatch latch = new CountDownLatch(threadCount);
            exceptions = Collections.synchronizedList(new ArrayList<>());

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    try {
                        walletService.transfer(
                                from.getUid(),
                                to.getUid(),
                                amountPerTransfer,
                                amountPerTransfer
                        );
                    } catch (Exception e) {
                        exceptions.add(e);
                    } finally {
                        latch.countDown();
                    }
                });
            }

            latch.await();
            executor.shutdown();
        }

        Wallet updatedFrom = walletRepository.findById(from.getUid()).orElseThrow();
        Wallet updatedTo = walletRepository.findById(to.getUid()).orElseThrow();

        BigDecimal expectedTransferred = amountPerTransfer.multiply(BigDecimal.valueOf(threadCount));
        BigDecimal actualTransferred = new BigDecimal("1000.00").subtract(updatedFrom.getBalance());

        assertEquals(expectedTransferred, actualTransferred, "Transferred amount mismatch");
        assertEquals(expectedTransferred, updatedTo.getBalance(), "Receiver balance mismatch");
        assertTrue(updatedFrom.getBalance().compareTo(BigDecimal.ZERO) >= 0, "Sender balance negative");

        if (!exceptions.isEmpty()) {
            fail("Some transfers failed unexpectedly: " + exceptions.stream()
                    .map(Throwable::getMessage)
                    .collect(Collectors.joining(", ")));
        }
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

    private Wallet createWallet(String name, WalletType type, BigDecimal balance) {
        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setWalletType(type);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus(WalletStatus.ACTIVE);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
        return wallet;
    }

    private Wallet createWallet(String name, WalletType type, BigDecimal balance, WalletStatus status) {
        Wallet wallet = new Wallet();
        wallet.setName(name);
        wallet.setWalletType(type);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setStatus(status);
        wallet.setBalance(balance);
        walletRepository.save(wallet);
        return wallet;
    }

}
