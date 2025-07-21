package com.advanced.transactionservice.controller.transaction.confirm;

import com.advanced.contract.model.TransferConfirmRequest;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.repository.TransactionRepository;
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
import org.springframework.test.context.ActiveProfiles;
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

import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ConfirmTransferRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @BeforeEach
    void setup() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void confirmTransfer_shouldTransferMoneyBetweenWallets() {
        BigDecimal initialBalance = new BigDecimal("500.00");
        BigDecimal transferAmount = new BigDecimal("200.00");

        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", initialBalance);
        Wallet toWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "to", BigDecimal.ZERO);

        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setWalletUid(fromWallet.getUid());
        request.setTargetWalletUid(toWallet.getUid());
        request.setAmount(transferAmount);
        request.setCurrency("RUB");
        request.setFee(BigDecimal.ZERO);
        request.setComment("Test transfer");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Wallet updatedFrom = walletRepository.findById(fromWallet.getUid()).orElseThrow();
        Wallet updatedTo = walletRepository.findById(toWallet.getUid()).orElseThrow();

        assertEquals(initialBalance.subtract(transferAmount), updatedFrom.getBalance());
        assertEquals(transferAmount, updatedTo.getBalance());

        assertEquals(1, transactionRepository.count());
    }

    @Test
    void confirmTransfer_shouldReturnNotFound_whenSourceWalletNotFound() {
        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setTargetWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));
        request.setFee(BigDecimal.ZERO);
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void confirmTransfer_shouldReturnNotFound_whenTargetWalletNotFound() {
        Wallet fromWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "from", BigDecimal.valueOf(500));

        TransferConfirmRequest request = new TransferConfirmRequest();
        request.setWalletUid(fromWallet.getUid());
        request.setTargetWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.valueOf(100));
        request.setFee(BigDecimal.ZERO);
        request.setCurrency("RUB");

        webTestClient.post()
                .uri("/api/v1/transactions/transfer/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void confirmTransfer_shouldReturnBadRequest_whenAmountIsZeroOrNegative() {
        Wallet sourceWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "same", BigDecimal.valueOf(100));
        Wallet targetWallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "same", BigDecimal.ZERO);

        List<BigDecimal> invalidAmounts = List.of(BigDecimal.ZERO, BigDecimal.valueOf(-10));

        for (BigDecimal invalidAmount : invalidAmounts) {
            TransferConfirmRequest request = new TransferConfirmRequest();
            request.setWalletUid(sourceWallet.getUid());
            request.setTargetWalletUid(targetWallet.getUid());
            request.setAmount(invalidAmount);
            request.setFee(BigDecimal.ZERO);
            request.setCurrency("RUB");
            request.setComment("Invalid amount");

            webTestClient.post()
                    .uri("/api/v1/transactions/transfer/confirm")
                    .bodyValue(request)
                    .exchange()
                    .expectStatus().isBadRequest();
        }
    }


}
