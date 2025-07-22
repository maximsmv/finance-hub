package com.advanced.transactionservice.controller.transaction.listener;

import com.advanced.kafkacontracts.DepositCompleted;
import com.advanced.kafkacontracts.WithdrawalCompleted;
import com.advanced.kafkacontracts.WithdrawalFailed;
import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.Transaction;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.repository.TransactionRepository;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@AutoConfigureWebTestClient
@Testcontainers
public class ListenerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private KafkaTopicsProperties kafkaTopics;

    private static Map<String, Object> producerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081));
        props.put("specific.avro.reader", "true");
        return props;
    }

    static KafkaTemplate<String, DepositCompleted> depositCompletedKafkaTemplate;
    static KafkaTemplate<String, WithdrawalCompleted> withdrawalCompletedKafkaTemplate;
    static KafkaTemplate<String, WithdrawalFailed> withdrawalFailedKafkaTemplate;

    @BeforeAll
    static void startContainers() {
        depositCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
        withdrawalCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
        withdrawalFailedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
    }

    @AfterEach
    void setup() {
        transactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void shouldProcessDepositCompletedEventAndCreditWallet() {
        UUID userUid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        BigDecimal fee = new BigDecimal("5.00");

        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", BigDecimal.ZERO, userUid);

        Transaction transaction = new Transaction();
        transaction.setWalletUid(wallet.getUid());
        transaction.setAmount(amount);
        transaction.setType(PaymentType.DEPOSIT);
        transaction.setFee(fee);
        transaction.setCurrency(Currency.getInstance("RUB"));
        transaction.setUserUid(userUid);
        transaction.setStatus(PaymentStatus.PENDING);
        transactionRepository.saveAndFlush(transaction);

        DepositCompleted event = DepositCompleted.newBuilder()
                .setTransactionUid(transaction.getUid())
                .setStatus("COMPLETED")
                .setAmount(new BigDecimal("95.00"))
                .setTimestamp(Instant.now())
                .build();
        depositCompletedKafkaTemplate.send(kafkaTopics.getDepositCompleted(), event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Transaction updated = transactionRepository.findById(transaction.getUid()).orElseThrow();
            assertEquals(PaymentStatus.COMPLETED, updated.getStatus());
            assertEquals(new BigDecimal("95.00"), walletRepository.findById(wallet.getUid()).orElseThrow().getBalance());
        });
    }

    @Test
    void shouldProcessWithdrawalCompletedEventAndUpdateTransactionStatus() {
        UUID userUid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("200.00");
        BigDecimal fee = new BigDecimal("10.00");

        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", new BigDecimal("1000.00"), userUid);

        Transaction transaction = new Transaction();
        transaction.setWalletUid(wallet.getUid());
        transaction.setAmount(amount);
        transaction.setWalletUid(wallet.getUid());
        transaction.setType(PaymentType.WITHDRAWAL);
        transaction.setFee(fee);
        transaction.setCurrency(Currency.getInstance("RUB"));
        transaction.setUserUid(userUid);
        transaction.setStatus(PaymentStatus.PENDING);
        transactionRepository.saveAndFlush(transaction);

        WithdrawalCompleted event = WithdrawalCompleted.newBuilder()
                .setTransactionUid(transaction.getUid())
                .setFailureReason(null)
                .setStatus("COMPLETED")
                .setTimestamp(Instant.now())
                .build();

        System.out.println("TOPIC = " + kafkaTopics.getWithdrawalCompleted());
        System.out.println("UID = " + transaction.getUid());
        withdrawalCompletedKafkaTemplate.send(kafkaTopics.getWithdrawalCompleted(), event);
        withdrawalCompletedKafkaTemplate.flush();

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Transaction updated = transactionRepository.findByUidAndUserUid(transaction.getUid(), transaction.getUserUid()).orElseThrow();
            assertEquals(PaymentStatus.COMPLETED, updated.getStatus());
        });
    }

    @Test
    void shouldProcessWithdrawalFailedEventAndRollbackBalance() {
        UUID userUid = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("300.00");
        BigDecimal fee = new BigDecimal("15.00");

        Wallet wallet = WalletUtils.createWallet(walletTypeRepository, walletRepository, "test", new BigDecimal("0.00"), userUid);

        Transaction transaction = new Transaction();
        transaction.setWalletUid(wallet.getUid());
        transaction.setAmount(amount);
        transaction.setWalletUid(wallet.getUid());
        transaction.setType(PaymentType.WITHDRAWAL);
        transaction.setFee(fee);
        transaction.setCurrency(Currency.getInstance("RUB"));
        transaction.setUserUid(userUid);
        transaction.setStatus(PaymentStatus.PENDING);
        transactionRepository.saveAndFlush(transaction);

        WithdrawalFailed event = WithdrawalFailed.newBuilder()
                .setTransactionUid(transaction.getUid())
                .setFailureReason("Insufficient funds")
                .setStatus("FAILED")
                .setTimestamp(Instant.now())
                .build();

        withdrawalFailedKafkaTemplate.send(kafkaTopics.getWithdrawalFailed(), event);

        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            Transaction updated = transactionRepository.findByUidAndUserUid(transaction.getUid(), transaction.getUserUid()).orElseThrow();
            assertEquals(PaymentStatus.FAILED, updated.getStatus());
            assertEquals("Insufficient funds", updated.getFailureReason());
            assertEquals(amount.add(fee), walletRepository.findByUidAndUserUid(wallet.getUid(), wallet.getUserUid()).orElseThrow().getBalance());
        });
    }
}
