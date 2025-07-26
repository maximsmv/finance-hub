package com.advanced.transactionservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.Map;

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
        registry.add("shards.SQL_SHOW",() -> "true");
        registry.add("SHARDS_CONFIG", AbstractIntegrationTest::buildShardsConfigJson);
    }

    private static String buildShardsConfigJson() {
        Map<String, Object> config = Map.of(
                "count", 2,
                "datasources", Map.of(
                        "ds_0", Map.of(
                                "jdbcUrl", POSTGRES_0.getJdbcUrl(),
                                "username", POSTGRES_0.getUsername(),
                                "password", POSTGRES_0.getPassword()
                        ),
                        "ds_1", Map.of(
                                "jdbcUrl", POSTGRES_1.getJdbcUrl(),
                                "username", POSTGRES_1.getUsername(),
                                "password", POSTGRES_1.getPassword()
                        )
                )
        );
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Ошибка парсинга конфигурации шардирования", e);
        }
    }
}
