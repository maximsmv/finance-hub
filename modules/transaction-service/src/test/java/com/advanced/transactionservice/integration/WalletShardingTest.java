package com.advanced.transactionservice.integration;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static com.advanced.transactionservice.utils.WalletUtils.getWalletType;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;


@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
public class WalletShardingTest extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void createWallet_shouldCreateSuccessfully() {
        WalletType type = walletTypeRepository.save(getWalletType(walletTypeRepository));

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

}
