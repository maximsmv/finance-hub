package com.advanced.transactionservice.controller.transaction.confirm;

import com.advanced.contract.model.WithdrawalConfirmRequest;
import com.advanced.kafka.contracts.model.WithdrawalRequestedPayload;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.utils.WalletUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.shaded.org.awaitility.core.ConditionTimeoutException;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ConfirmWithdrawalRestControllerV1Test {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private KafkaTopicsProperties kafkaTopicsProperties;

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

    @Container
    static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.7.0")
    )
            .withNetwork(network)
            .withNetworkAliases("kafka");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }

    @BeforeAll
    static void startContainers() {
        kafka.start();
        postgres.start();

        try (AdminClient admin = AdminClient.create(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
        ))) {
            admin.listTopics().names().get(10, TimeUnit.SECONDS);
            System.out.println("Kafka is available");
        } catch (Exception e) {
            System.err.println("Kafka is not available: " + e.getMessage());
            throw new RuntimeException("Kafka is not available", e);
        }

    }

    @AfterAll
    static void tearDown() {
        kafka.stop();
        postgres.stop();
    }

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
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        consumerProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        consumerProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, WithdrawalRequestedPayload.class.getName());

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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(new BigDecimal("50.00"));

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
                    assertInstanceOf(WithdrawalRequestedPayload.class, record.value());

                    WithdrawalRequestedPayload payload = (WithdrawalRequestedPayload) record.value();
                    assertEquals(wallet.getUid().toString(), payload.getWalletId());
                    assertEquals("RUB", payload.getCurrency());
                    assertEquals("150.00", payload.getAmount());
                    assertEquals(wallet.getUserUid().toString(), payload.getUserId());
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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(fee);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofMillis(500));
                    assertEquals(1, countRecordsWithTransactionUid(records, request.getTransactionUid()));
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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.ZERO);

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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.ZERO);

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
        request.setTransactionUid(UUID.randomUUID());

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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(new BigDecimal("0"));

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
        request.setTransactionUid(UUID.randomUUID());

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
        request.setTransactionUid(UUID.randomUUID());

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.ZERO);

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

    @Test
    void confirmWithdrawal_shouldBeIdempotent_whenSameTransactionUidIsUsed() {
        UUID transactionUid = UUID.randomUUID();
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "withdrawal-idempotent", BigDecimal.valueOf(500));


        WithdrawalConfirmRequest request = new WithdrawalConfirmRequest();
        request.setWalletUid(wallet.getUid());
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");
        request.setComment("Idempotency test - withdrawal");
        request.setTransactionUid(transactionUid);

        when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.ZERO);

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk();

        Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, Object> records = kafkaConsumer.poll(Duration.ofMillis(1000));
                    assertEquals(1, countRecordsWithTransactionUid(records, transactionUid),
                            "Should find exactly one message with transactionUid: " + transactionUid);
                });

        webTestClient.post()
                .uri("/api/v1/transactions/withdrawal/confirm")
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
                .filter(WithdrawalRequestedPayload.class::isInstance)
                .map(WithdrawalRequestedPayload.class::cast)
                .filter(p -> transactionUid.toString().equals(p.getTransactionId()))
                .count();
    }

}
