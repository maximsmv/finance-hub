package com.advanced.transactionservice.controller.transaction.listener;

import com.advanced.kafka.contracts.model.DepositCompletedPayload;
import com.advanced.kafka.contracts.model.FailureReason;
import com.advanced.kafka.contracts.model.WithdrawalCompletedPayload;
import com.advanced.kafka.contracts.model.WithdrawalFailedPayload;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.shaded.org.awaitility.Awaitility;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ListenerIT {

    @Autowired
    private PaymentRequestRepository paymentRequestRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private KafkaTopicsProperties kafkaTopics;

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

    private static Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return props;
    }

    static KafkaTemplate<String, DepositCompletedPayload> depositCompletedKafkaTemplate;
    static KafkaTemplate<String, WithdrawalCompletedPayload> withdrawalCompletedKafkaTemplate;
    static KafkaTemplate<String, WithdrawalFailedPayload> withdrawalFailedKafkaTemplate;

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

        depositCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
        withdrawalCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
        withdrawalFailedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
    }

    @AfterAll
    static void tearDown() {
        kafka.stop();
        postgres.stop();
    }

    @BeforeEach
    void setup() {
        paymentRequestRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void listener_shouldProcessDepositCompletedEvent() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO);

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.DEPOSIT);
        paymentRequestRepository.save(request);

        DepositCompletedPayload payload = new DepositCompletedPayload();
        payload.setTransactionId(transactionUid.toString());

        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid)
                            .orElseThrow();
                    assertEquals(PaymentStatus.COMPLETED, updated.getStatus());
                    assertNotNull(updated.getProcessedAt());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("100.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void listener_shouldProcessWithdrawalCompletedEvent() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(200));

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.WITHDRAWAL);
        paymentRequestRepository.save(request);

        WithdrawalCompletedPayload payload = new WithdrawalCompletedPayload();
        payload.setTransactionId(transactionUid.toString());

        withdrawalCompletedKafkaTemplate.send(kafkaTopics.getWithdrawalCompleted(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid).orElseThrow();
                    assertEquals(PaymentStatus.COMPLETED, updated.getStatus());
                    assertNotNull(updated.getProcessedAt());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("100.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void listener_shouldProcessWithdrawalFailedEvent() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.valueOf(300));

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("150.00"));
        request.setTotalAmount(new BigDecimal("150.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.WITHDRAWAL);
        paymentRequestRepository.save(request);

        WithdrawalFailedPayload payload = new WithdrawalFailedPayload();
        payload.setTransactionId(transactionUid.toString());
        payload.setReason(FailureReason.UNKNOWN_ERROR);

        withdrawalFailedKafkaTemplate.send(kafkaTopics.getWithdrawalFailed(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid).orElseThrow();
                    assertEquals(PaymentStatus.FAILED, updated.getStatus());
                    assertNotNull(updated.getProcessedAt());
                    assertEquals("UNKNOWN_ERROR", updated.getFailureReason());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("300.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void depositCompletedListener_shouldIgnoreEvent_whenTransactionNotFound() {
        UUID nonExistentTransactionUid = UUID.randomUUID();

        DepositCompletedPayload payload = new DepositCompletedPayload();
        payload.setTransactionId(nonExistentTransactionUid.toString());

        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), payload);

        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    boolean exists = paymentRequestRepository.findByTransactionUid(nonExistentTransactionUid).isPresent();
                    assertFalse(exists, "PaymentRequest по несуществующему transactionId не должен появиться");
                });
    }

    @Test
    void withdrawalCompletedListener_shouldIgnoreEvent_whenTransactionNotFound() {
        UUID nonExistentTransactionUid = UUID.randomUUID();

        WithdrawalCompletedPayload payload = new WithdrawalCompletedPayload();
        payload.setTransactionId(nonExistentTransactionUid.toString());

        withdrawalCompletedKafkaTemplate.send(kafkaTopics.getWithdrawalCompleted(), payload);

        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    boolean exists = paymentRequestRepository.findByTransactionUid(nonExistentTransactionUid).isPresent();
                    assertFalse(exists);
                });
    }

    @Test
    void withdrawalFailedListener_shouldIgnoreEvent_whenTransactionNotFound() {
        UUID nonExistentTransactionUid = UUID.randomUUID();

        WithdrawalFailedPayload payload = new WithdrawalFailedPayload();
        payload.setTransactionId(nonExistentTransactionUid.toString());
        payload.setReason(FailureReason.UNKNOWN_ERROR);

        withdrawalFailedKafkaTemplate.send(kafkaTopics.getWithdrawalFailed(), payload);

        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    boolean exists = paymentRequestRepository.findByTransactionUid(nonExistentTransactionUid).isPresent();
                    assertFalse(exists);
                });
    }

    @Test
    void depositCompletedListener_shouldBeIdempotent_whenSameTransactionIdIsRepeated() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO);

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.DEPOSIT);
        paymentRequestRepository.save(request);

        DepositCompletedPayload payload = new DepositCompletedPayload();
        payload.setTransactionId(transactionUid.toString());

        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), payload);
        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid).orElseThrow();
                    assertEquals(PaymentStatus.COMPLETED, updated.getStatus());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("100.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void withdrawalCompletedListener_shouldBeIdempotent_whenSameTransactionIdIsRepeated() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", new BigDecimal("200.00"));

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.WITHDRAWAL);
        paymentRequestRepository.save(request);

        WithdrawalCompletedPayload payload = new WithdrawalCompletedPayload();
        payload.setTransactionId(transactionUid.toString());

        withdrawalCompletedKafkaTemplate.send(kafkaTopics.getWithdrawalCompleted(), payload);
        withdrawalCompletedKafkaTemplate.send(kafkaTopics.getWithdrawalCompleted(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid).orElseThrow();
                    assertEquals(PaymentStatus.COMPLETED, updated.getStatus());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("100.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void withdrawalFailedListener_shouldBeIdempotent_whenSameTransactionIdIsRepeated() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", new BigDecimal("200.00"));

        UUID transactionUid = UUID.randomUUID();

        PaymentRequest request = new PaymentRequest();
        request.setTransactionUid(transactionUid);
        request.setWalletUid(wallet.getUid());
        request.setUserUid(wallet.getUserUid());
        request.setAmount(new BigDecimal("100.00"));
        request.setTotalAmount(new BigDecimal("100.00"));
        request.setStatus(PaymentStatus.PENDING);
        request.setCurrency(Currency.getInstance("RUB"));
        request.setType(PaymentType.WITHDRAWAL);
        paymentRequestRepository.save(request);

        WithdrawalFailedPayload payload = new WithdrawalFailedPayload();
        payload.setTransactionId(transactionUid.toString());
        payload.setReason(FailureReason.UNKNOWN_ERROR);

        withdrawalFailedKafkaTemplate.send(kafkaTopics.getWithdrawalFailed(), payload);
        withdrawalFailedKafkaTemplate.send(kafkaTopics.getWithdrawalFailed(), payload);

        Awaitility.await()
                .atMost(Duration.ofSeconds(10))
                .untilAsserted(() -> {
                    PaymentRequest updated = paymentRequestRepository.findByTransactionUid(transactionUid).orElseThrow();
                    assertEquals(PaymentStatus.FAILED, updated.getStatus());

                    Wallet updatedWallet = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("200.00"), updatedWallet.getBalance());
                });
    }

    @Test
    void depositCompletedListener_shouldNotProcess_whenTransactionIdIsNull() {
        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO);

        DepositCompletedPayload payload = new DepositCompletedPayload();
        payload.setTransactionId(null);

        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), payload);

        Awaitility.await()
                .during(Duration.ofSeconds(2))
                .atMost(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    long count = paymentRequestRepository.count();
                    assertEquals(0, count, "Не должно быть сохранённых платежей при невалидном payload");
                    Wallet w = walletRepository.findById(wallet.getUid()).orElseThrow();
                    assertEquals(new BigDecimal("0.00"), w.getBalance(), "Баланс не должен был измениться");
                });
    }
}
