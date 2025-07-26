package com.advanced.transactionservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.shardingsphere.broadcast.config.BroadcastRuleConfiguration;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Configuration
public class ShardingSphereConfiguration {

    @Value("${SHARDS_CONFIG:}")
    private String shardsConfigJson;

    @Value("${SHARDS_CONFIG_PATH:}")
    private String shardsConfigPath;

    @Value("${SQL_SHOW:true}")
    private boolean sqlShow;

    @Getter
    private final Map<String, DataSourceProperties> datasources = new HashMap<>();

    @Bean
    public DataSource dataSource() throws SQLException {
        ShardSettings shardSettings = loadShardSettings();

        Map<String, DataSource> dataSourceMap = new HashMap<>();
        int count = shardSettings.getCount();

        for (int i = 0; i < count; i++) {
            String name = "ds_" + i;
            DataSourceProperties props = shardSettings.getDatasources().get(name);
            if (props == null) {
                log.error("Missing datasource config for {}",  name);
                throw new IllegalStateException("Missing datasource config for " + name);
            }
            datasources.put(name, props);
            dataSourceMap.put(name, createDataSource(props));
        }

        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        String actualDataNodesExpr = "ds_${0.." + (count - 1) + "}";

        ShardingTableRuleConfiguration transactionsRule = new ShardingTableRuleConfiguration(
                "transactions", actualDataNodesExpr + ".transactions");
        transactionsRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_uid", "database_inline"));

        ShardingTableRuleConfiguration walletsRule = new ShardingTableRuleConfiguration(
                "wallets", actualDataNodesExpr + ".wallets");
        walletsRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_uid", "database_inline"));

        shardingRuleConfig.getTables().add(transactionsRule);
        shardingRuleConfig.getTables().add(walletsRule);

        Properties algorithmProps = new Properties();
        algorithmProps.setProperty("algorithm-expression", "ds_${(user_uid.hashCode() % " + count + " + " + count + ") % " + count + "}");
        shardingRuleConfig.getShardingAlgorithms()
                .put("database_inline", new AlgorithmConfiguration("INLINE", algorithmProps));

        shardingRuleConfig.setDefaultDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_uid", "database_inline"));

        BroadcastRuleConfiguration broadcastRuleConfig = new BroadcastRuleConfiguration(
                Collections.singletonList("wallet_types"));

        Properties props = new Properties();
        props.setProperty("sql-show", String.valueOf(sqlShow));

        return ShardingSphereDataSourceFactory.createDataSource(
                dataSourceMap,
                Arrays.asList(shardingRuleConfig, broadcastRuleConfig),
                props
        );
    }

    private DataSource createDataSource(DataSourceProperties props) {
        HikariDataSource ds = new HikariDataSource();
        ds.setDriverClassName("org.postgresql.Driver");
        ds.setJdbcUrl(props.getJdbcUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        return ds;
    }

    @Setter
    @Getter
    public static class DataSourceProperties {
        private String jdbcUrl;
        private String username;
        private String password;
    }

    private ShardSettings loadShardSettings() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            if (shardsConfigJson != null && !shardsConfigJson.isBlank()) {
                log.info("Загружаю SHARDS_CONFIG из строки окружения");
                return mapper.readValue(shardsConfigJson, ShardSettings.class);
            } else if (shardsConfigPath != null && !shardsConfigPath.isBlank()) {
                log.info("Загружаю SHARDS_CONFIG из файла: {}", shardsConfigPath);

                InputStream is;

                Path path = Path.of(shardsConfigPath);
                if (Files.exists(path)) {
                    is = Files.newInputStream(path);
                } else {
                    is = getClass().getClassLoader().getResourceAsStream(shardsConfigPath);
                    if (is == null) {
                        log.error("Файл не найден: {}", shardsConfigPath);
                        throw new IllegalStateException("Файл не найден: " + shardsConfigPath);
                    }
                }

                return mapper.readValue(is, ShardSettings.class);
            } else {
                log.error("Ни SHARDS_CONFIG, ни SHARDS_CONFIG_PATH не заданы");
                throw new IllegalStateException("Ни SHARDS_CONFIG, ни SHARDS_CONFIG_PATH не заданы");
            }
        } catch (Exception e) {
            log.error("Ошибка при загрузке SHARDS_CONFIG");
            throw new IllegalStateException("Ошибка при загрузке SHARDS_CONFIG", e);
        }
    }
}

