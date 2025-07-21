package com.advanced.transactionservice.controller.transaction.confirm;

import com.advanced.contract.model.WithdrawalConfirmRequest;
import com.advanced.kafkacontracts.WithdrawalRequested;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.repository.TransactionRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ConfirmWithdrawalRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

    private static Consumer<String, Object> kafkaConsumer;

    @BeforeEach
    void setup() {
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, "test-group-" + UUID.randomUUID());
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");
        consumerProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 15000);
        consumerProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 30000);
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        consumerProps.put("schema.registry.url", "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081));
        consumerProps.put("specific.avro.reader", "true");
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");

        kafkaConsumer = new DefaultKafkaConsumerFactory<String, Object>(consumerProps).createConsumer();

        kafkaConsumer.subscribe(List.of(kafkaTopicsProperties.getWithdrawalRequested()));

        try {
            Awaitility.await()
                    .atMost(Duration.ofSeconds(30))
                    .pollInterval(Duration.ofSeconds(1))
                    .until(() -> {
                        kafkaConsumer.poll(Duration.ofMillis(100));
                        return !kafkaConsumer.assignment().isEmpty();
                    });
        } catch (ConditionTimeoutException e) {
            System.err.println("Partitions not assigned. Current assignment: " + kafkaConsumer.assignment());
            throw e;
        }

        kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());
        kafkaConsumer.poll(Duration.ofSeconds(1));
    }

    @AfterEach
    void tearDownKafkaConsumer() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
        if (kafkaConsumer != null) {
            kafkaConsumer.unsubscribe();
            kafkaConsumer.close(Duration.ofSeconds(1));
            kafkaConsumer = null;
        }
    }

    @Test
    void confirmWithdrawal_shouldSendMessageToKafka() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "withdraw", BigDecimal.valueOf(1000));

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setComment("withdraw test");
        request.setAmount(new BigDecimal("100.00"));
        request.setFee(new BigDecimal("50.00"));

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(
                            kafkaConsumer,
                            kafkaTopicsProperties.getWithdrawalRequested()
                    );
                    assertNotNull(record);
                    assertInstanceOf(WithdrawalRequested.class, record.value());

                    WithdrawalRequested payload = (WithdrawalRequested) record.value();
                    assertEquals(wallet.getUid(), payload.getWalletUid());
                    assertEquals("RUB", payload.getCurrency());
                    assertEquals(new BigDecimal("100.00"), payload.getAmount());
                    assertEquals(wallet.getUserUid(), payload.getUserUid());
                });
    }

    @Test
    void confirmWithdrawal_shouldSucceed_whenBalanceExactlyEqualsAmountPlusFee() {
        BigDecimal fee = new BigDecimal("10.00");
        BigDecimal amount = new BigDecimal("90.00");
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "exact-match", amount.add(fee));

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(amount);
        request.setComment("edge case match");
        request.setFee(fee);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofMillis(500));
                    assertEquals(1, countRecordsWithTransactionUid(records, transactionRepository.findAll().getFirst().getUid()));
                });
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenWalletIsBlocked() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "blocked", BigDecimal.valueOf(500), WalletStatus.BLOCKED);

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(100));
        request.setComment("blocked wallet");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenWalletIsArchived() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "archived", BigDecimal.valueOf(300), WalletStatus.ARCHIVED);

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(100));
        request.setComment("archived wallet");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenAmountIsNegative() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "negative-amount", BigDecimal.valueOf(500));

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(-50));
        request.setComment("negative");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenInsufficientFunds() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(10));

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");
        request.setComment("too much");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenAmountIsMissing() {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnUnsupportedMediaType_whenRequestBodyIsEmpty() {
        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.UNSUPPORTED_MEDIA_TYPE);

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenWalletDoesNotExist() {
        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(100));
        request.setComment("not found");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmWithdrawal_shouldReturnBadRequest_whenTransactionUidIsMissing() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "null-uid", BigDecimal.valueOf(200));

        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.valueOf(50));
        request.setComment("no transactionUid");

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    private void assertNoKafkaMessagesSent() {
        kafkaConsumer.seekToBeginning(kafkaConsumer.assignment());

        Awaitility.await()
                .atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofMillis(100));
                    assertTrue(records.isEmpty(), "Kafka was not expected to receive the messages, but they were sent.");
                });
    }

    private long countRecordsWithTransactionUid(ConsumerRecords<String, Object> records, UUID transactionUid) {
        return StreamSupport.stream(records.spliterator(), false)
                .map(ConsumerRecord::value)
                .filter(WithdrawalRequested.class::isInstance)
                .map(WithdrawalRequested.class::cast)
                .filter(p -> transactionUid.equals(p.getTransactionUid()))
                .count();
    }

}
