package com.advanced.transactionservice;

import org.flywaydb.core.Flyway;
import org.junit.Before;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Testcontainers
public abstract class AbstractIntegrationTest {

    public static class FixedPortPostgreSQLContainer extends PostgreSQLContainer<FixedPortPostgreSQLContainer> {
        public FixedPortPostgreSQLContainer(String dockerImageName) {
            super(dockerImageName);
        }

        public FixedPortPostgreSQLContainer withFixedExposedPort(int hostPort, int containerPort) {
            super.addFixedExposedPort(hostPort, containerPort);
            return this;
        }

        @Override
        public void start() {
            super.start();
            Flyway.configure()
                    .dataSource(getJdbcUrl(), getUsername(), getPassword())
                    .load()
                    .migrate();
        }
    }

    private static final Network network = Network.newNetwork();

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_0 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(65431, 5432)
            .withDatabaseName("transactdb_0")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true);

    @Container
    public static FixedPortPostgreSQLContainer POSTGRES_1 = new FixedPortPostgreSQLContainer("postgres:latest")
            .withFixedExposedPort(65432, 5432)
            .withDatabaseName("transactdb_1")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true);

    @Container
    public static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.7.0")
    )
            .withNetwork(network)
            .withNetworkAliases("kafka")
            .withReuse(true);

    @Container
    public static GenericContainer<?> schemaRegistry = new GenericContainer<>(
            DockerImageName.parse("confluentinc/cp-schema-registry:7.7.0")
    )
            .withNetwork(network)
            .withNetworkAliases("schema-registry")
            .withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry")
            .withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", kafka.getNetworkAliases().getFirst() + ":9092")
            .withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:8081")
            .withExposedPorts(8081)
            .dependsOn(kafka)
            .withReuse(true);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registryShardingDatasources(registry);
    }

    private static void registryShardingDatasources(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.properties.schema.registry.url", () ->
                "http://" + schemaRegistry.getHost() + ":" + schemaRegistry.getMappedPort(8081));
        registry.add("DS_0_JDBC_URL",() -> POSTGRES_0.getJdbcUrl());
        registry.add("DS_0_USERNAME",() -> POSTGRES_0.getUsername());
        registry.add("DS_0_PASSWORD",() -> POSTGRES_0.getPassword());
        registry.add("DS_1_JDBC_URL",() -> POSTGRES_1.getJdbcUrl());
        registry.add("DS_1_USERNAME",() -> POSTGRES_1.getUsername());
        registry.add("DS_1_PASSWORD",() -> POSTGRES_1.getPassword());
        registry.add("SQL_SHOW",() -> "true");

    }
}
