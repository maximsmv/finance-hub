package com.advanced.transactionservice.configuration;

import com.zaxxer.hikari.HikariDataSource;
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
import java.sql.SQLException;
import java.util.*;

@Configuration
public class ShardingSphereConfig {

    @Value("${DS_0_JDBC_URL:jdbc:postgresql://transaction-service-postgres-0:5432/transaction_db_0}")
    private String ds0JdbcUrl;
    @Value("${DS_0_USERNAME:transaction-service}")
    private String ds0Username;
    @Value("${DS_0_PASSWORD:password}")
    private String ds0Password;

    @Value("${DS_1_JDBC_URL:jdbc:postgresql://transaction-service-postgres-1:5432/transaction_db_1}")
    private String ds1JdbcUrl;
    @Value("${DS_1_USERNAME:transaction-service}")
    private String ds1Username;
    @Value("${DS_1_PASSWORD:password}")
    private String ds1Password;

    @Value("${SQL_SHOW:true}")
    private boolean sqlShow;

    @Bean
    public DataSource dataSource() throws SQLException {
        Map<String, DataSource> dataSourceMap = new HashMap<>();
        dataSourceMap.put("ds_0", createDataSource(ds0JdbcUrl, ds0Username, ds0Password));
        dataSourceMap.put("ds_1", createDataSource(ds1JdbcUrl, ds1Username, ds1Password));

        ShardingRuleConfiguration shardingRuleConfig = new ShardingRuleConfiguration();

        ShardingTableRuleConfiguration transactionsRule = new ShardingTableRuleConfiguration(
                "transactions", "ds_${0..1}.transactions");
        transactionsRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_uid", "database_inline"));

        ShardingTableRuleConfiguration walletsRule = new ShardingTableRuleConfiguration(
                "wallets", "ds_${0..1}.wallets");
        walletsRule.setDatabaseShardingStrategy(
                new StandardShardingStrategyConfiguration("user_uid", "database_inline"));

        shardingRuleConfig.getTables().add(transactionsRule);
        shardingRuleConfig.getTables().add(walletsRule);

        Properties algorithmProps = new Properties();
        algorithmProps.setProperty("algorithm-expression", "ds_${(user_uid.hashCode() % 2 + 2) % 2}");
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
                props);
    }

    private DataSource createDataSource(String jdbcUrl, String username, String password) {
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return dataSource;
    }

}
