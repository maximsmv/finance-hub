package com.advanced.transactionservice.integration;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class WalletRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @AfterEach
    void setup() {
        walletRepository.deleteAll();
    }

    @Test
    void createWallet_shouldCreateSuccessfully() {
        UUID userUid = UUID.randomUUID();

        var request = new CreateWalletRequest();
        request.setName("Test Wallet");
        request.setUserUid(userUid);
        request.setWalletTypeUid(UUID.fromString("e32bd41e-bb27-4942-adce-f2b406aa5f3e"));

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
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "Test", new BigDecimal("100.00"));

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
        UUID userUid = UUID.randomUUID();

        WalletUtils.createWallet(walletTypeRepository, walletRepository, "W1", new BigDecimal("10.00"), userUid);
        WalletUtils.createWallet(walletTypeRepository, walletRepository, "W2", new BigDecimal("20.00"), userUid);

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
}