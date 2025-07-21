//package com.advanced.transactionservice.controller.transaction.listener;
//
//import com.advanced.kafkacontracts.DepositCompleted;
//import com.advanced.kafkacontracts.WithdrawalCompleted;
//import com.advanced.kafkacontracts.WithdrawalFailed;
//import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
//import com.advanced.transactionservice.repository.TransactionRepository;
//import com.advanced.transactionservice.repository.WalletRepository;
//import com.advanced.transactionservice.repository.WalletTypeRepository;
//import org.apache.kafka.clients.admin.AdminClient;
//import org.apache.kafka.clients.consumer.ConsumerConfig;
//import org.apache.kafka.clients.producer.ProducerConfig;
//import org.apache.kafka.common.serialization.StringSerializer;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.kafka.core.DefaultKafkaProducerFactory;
//import org.springframework.kafka.core.KafkaTemplate;
//import org.springframework.kafka.support.serializer.JsonSerializer;
//import org.springframework.test.context.DynamicPropertyRegistry;
//import org.springframework.test.context.DynamicPropertySource;
//import org.springframework.test.context.junit.jupiter.SpringExtension;
//import org.testcontainers.containers.Network;
//import org.testcontainers.containers.PostgreSQLContainer;
//import org.testcontainers.junit.jupiter.Container;
//import org.testcontainers.junit.jupiter.Testcontainers;
//import org.testcontainers.kafka.KafkaContainer;
//import org.testcontainers.utility.DockerImageName;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;
//
//@ExtendWith(SpringExtension.class)
//@SpringBootTest(
//        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
//)
//@AutoConfigureWebTestClient
//@Testcontainers
//public class ListenerIT {
//
//    @Autowired
//    private TransactionRepository transactionRepository;
//
//    @Autowired
//    private WalletRepository walletRepository;
//
//    @Autowired
//    private KafkaTopicsProperties kafkaTopics;
//
//    @Autowired
//    private WalletTypeRepository walletTypeRepository;
//
//    private static final Network network = Network.newNetwork();
//
//    @Container
//    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15")
//            .withExposedPorts(5432)
//            .withDatabaseName("testdb")
//            .withUsername("user")
//            .withPassword("pass")
//            .withNetwork(network)
//            .withNetworkAliases("postgres");
//
//    @Container
//    static KafkaContainer kafka = new KafkaContainer(
//            DockerImageName.parse("apache/kafka:3.7.0")
//    )
//            .withNetwork(network)
//            .withNetworkAliases("kafka");
//
//    @DynamicPropertySource
//    static void overrideProps(DynamicPropertyRegistry registry) {
//        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
//        registry.add("spring.datasource.url", postgres::getJdbcUrl);
//        registry.add("spring.datasource.username", postgres::getUsername);
//        registry.add("spring.datasource.password", postgres::getPassword);
//        registry.add("spring.flyway.url", postgres::getJdbcUrl);
//        registry.add("spring.flyway.user", postgres::getUsername);
//        registry.add("spring.flyway.password", postgres::getPassword);
//    }
//
//    private static Map<String, Object> producerConfigs() {
//        Map<String, Object> props = new HashMap<>();
//        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
//        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
//        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
//        return props;
//    }
//
//    static KafkaTemplate<String, DepositCompleted> depositCompletedKafkaTemplate;
//    static KafkaTemplate<String, WithdrawalCompleted> withdrawalCompletedKafkaTemplate;
//    static KafkaTemplate<String, WithdrawalFailed> withdrawalFailedKafkaTemplate;
//
//    @BeforeAll
//    static void startContainers() {
//        kafka.start();
//        postgres.start();
//
//        try (AdminClient admin = AdminClient.create(Map.of(
//                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers()
//        ))) {
//            admin.listTopics().names().get(10, TimeUnit.SECONDS);
//            System.out.println("Kafka is available");
//        } catch (Exception e) {
//            System.err.println("Kafka is not available: " + e.getMessage());
//            throw new RuntimeException("Kafka is not available", e);
//        }
//
//        depositCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
//        withdrawalCompletedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
//        withdrawalFailedKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(producerConfigs()));
//    }
//
//    @AfterAll
//    static void tearDown() {
//        kafka.stop();
//        postgres.stop();
//    }
//
//    @BeforeEach
//    void setup() {
//        transactionRepository.deleteAll();
//        walletRepository.deleteAll();
//    }
//
//}
