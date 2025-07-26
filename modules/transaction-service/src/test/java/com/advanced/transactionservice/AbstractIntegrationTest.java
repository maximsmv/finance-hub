package com.advanced.transactionservice;

import org.flywaydb.core.Flyway;
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

    public static class FlywayMigrateAndPostgreSQLContainer extends PostgreSQLContainer<FlywayMigrateAndPostgreSQLContainer> {
        public FlywayMigrateAndPostgreSQLContainer(String dockerImageName) {
            super(dockerImageName);
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
    public static FlywayMigrateAndPostgreSQLContainer POSTGRES_0 = new FlywayMigrateAndPostgreSQLContainer("postgres:latest")
            .withDatabaseName("transactdb_0")
            .withUsername("postgres")
            .withPassword("test")
            .withReuse(true);

    @Container
    public static FlywayMigrateAndPostgreSQLContainer POSTGRES_1 = new FlywayMigrateAndPostgreSQLContainer("postgres:latest")
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
        registry.add("shards.count",() -> 2);
        registry.add("shards.datasources.ds_0.jdbc_url",() -> POSTGRES_0.getJdbcUrl());
        registry.add("shards.datasources.ds_0.username",() -> POSTGRES_0.getUsername());
        registry.add("shards.datasources.ds_0.password",() -> POSTGRES_0.getPassword());
        registry.add("shards.datasources.ds_1.jdbc_url",() -> POSTGRES_1.getJdbcUrl());
        registry.add("shards.datasources.ds_1.username",() -> POSTGRES_1.getUsername());
        registry.add("shards.datasources.ds_1.password",() -> POSTGRES_1.getPassword());
        registry.add("shards.SQL_SHOW",() -> "true");

    }
}
