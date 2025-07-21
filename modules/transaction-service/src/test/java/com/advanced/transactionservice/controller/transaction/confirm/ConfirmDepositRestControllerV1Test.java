package com.advanced.transactionservice.controller.transaction.confirm;

import com.advanced.contract.model.DepositConfirmRequest;
import com.advanced.kafkacontracts.DepositRequested;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
class ConfirmDepositRestControllerV1Test extends AbstractIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletRepository walletRepository;

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

        kafkaConsumer.subscribe(List.of(kafkaTopicsProperties.getDepositRequested()));

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
        if (kafkaConsumer != null) {
            kafkaConsumer.unsubscribe();
            kafkaConsumer.close(Duration.ofSeconds(1));
            kafkaConsumer = null;
        }
    }

    @Test
    void confirmDeposit_shouldSendMessageToKafka() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO);

        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setCurrency("RUB");
        request.setComment("test_comment");
        request.setAmount(new BigDecimal("10.00"));
        request.setFee(new BigDecimal("10.00"));

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    ConsumerRecord<String, Object> record = KafkaTestUtils.getSingleRecord(
                            kafkaConsumer,
                            kafkaTopicsProperties.getDepositRequested()
                    );
                    assertNotNull(record);
                    assertInstanceOf(DepositRequested.class, record.value());

                    DepositRequested payload = (DepositRequested) record.value();
                    assertEquals(wallet.getUid(), payload.getWalletUid());
                    assertNotNull(payload.getTransactionUid());
                    assertEquals(request.getCurrency(), payload.getCurrency());
                    assertEquals(wallet.getUserUid(), payload.getUserUid());
                    assertEquals(new BigDecimal("20.00"), payload.getAmount());
                });
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenWalletIsBlocked() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO, WalletStatus.BLOCKED);

        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.TEN);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.error").isNotEmpty();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenAmountIsMissing() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenCurrencyIsMissing() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setAmount(BigDecimal.TEN);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenWalletUidIsMissing() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.TEN);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenAmountIsZero() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.ZERO);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenAmountIsNegative() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setAmount(new BigDecimal("-10.00"));
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenCurrencyIsInvalid() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RU1"); // или "123"
        request.setAmount(BigDecimal.TEN);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnBadRequest_whenRequestBodyIsEmpty() {
        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{}")
                .exchange()
                .expectStatus().isBadRequest();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldReturnNotFound_whenWalletDoesNotExist() {
        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(UUID.randomUUID());
        request.setCurrency("RUB");
        request.setAmount(BigDecimal.TEN);
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        assertNoKafkaMessagesSent();
    }

    @Test
    void confirmDeposit_shouldBeIdempotent_whenSameTransactionUidIsUsed() {
        UUID transactionUid = UUID.randomUUID();
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO);

        DepositConfirmRequest request = new DepositConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");
        request.setComment("Idempotency test");
        request.setFee(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                    System.out.println("Polled records count: " + records.count());
                    records.forEach(r -> System.out.println("Record: " + r.value()));
                    assertEquals(1, countRecordsWithTransactionUid(records, transactionUid),
                            "Should find exactly one message with transactionUid: " + transactionUid);
                });

        webTestClient.post()
                .uri("/api/v1/transactions/deposit/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);

        ConsumerRecords<String, Object> newRecords = kafkaConsumer.poll(Duration.ofMillis(2000));
        assertEquals(0, countRecordsWithTransactionUid(newRecords, transactionUid),
                "No new messages should be sent for duplicate request");
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
                .filter(DepositRequested.class::isInstance)
                .map(DepositRequested.class::cast)
                .filter(p -> transactionUid.equals(p.getTransactionUid()))
                .count();
    }


}