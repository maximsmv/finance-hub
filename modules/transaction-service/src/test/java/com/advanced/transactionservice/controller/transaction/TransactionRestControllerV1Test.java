package com.advanced.transactionservice.controller.transaction;

import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.Transaction;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.repository.TransactionRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class TransactionRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void shouldReturnFilteredTransactions() {
        UUID userUid = UUID.randomUUID();
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.TEN, userUid);

        Transaction transaction = new Transaction();
        transaction.setWalletUid(wallet.getUid());
        transaction.setAmount(new BigDecimal("123.45"));
        transaction.setType(PaymentType.DEPOSIT);
        transaction.setFee(new BigDecimal("0.45"));
        transaction.setCurrency(Currency.getInstance("RUB"));
        transaction.setUserUid(userUid);
        transaction.setStatus(PaymentStatus.PENDING);
        transactionRepository.saveAndFlush(transaction);

        String url = UriComponentsBuilder.fromPath("api/v1/transactions")
                .queryParam("userUid", userUid)
                .queryParam("type", "DEPOSIT")
                .queryParam("status", "PENDING")
                .build().toUriString();

        webTestClient.get()
                .uri(url)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].transactionUid").isEqualTo(transaction.getUid().toString())
                .jsonPath("$[0].type").isEqualTo("DEPOSIT")
                .jsonPath("$[0].status").isEqualTo("PENDING");
    }

}
