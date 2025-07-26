package com.advanced.transactionservice.configuration;

import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import lombok.Setter;
import org.apache.shardingsphere.broadcast.config.BroadcastRuleConfiguration;
import org.apache.shardingsphere.driver.api.ShardingSphereDataSourceFactory;
import org.apache.shardingsphere.infra.algorithm.core.config.AlgorithmConfiguration;
import org.apache.shardingsphere.sharding.api.config.ShardingRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.rule.ShardingTableRuleConfiguration;
import org.apache.shardingsphere.sharding.api.config.strategy.sharding.StandardShardingStrategyConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

@Configuration
@ConfigurationProperties(prefix = "shards")
public class ShardingSphereConfiguration {

    @Setter
    private int count;

    @Setter
    private Map<String, DataSourceProperties> datasources = new HashMap<>();

    @Value("${SQL_SHOW:true}")
    private boolean sqlShow;

    @Bean
    public DataSource dataSource() throws SQLException {
        Map<String, DataSource> dataSourceMap = new HashMap<>();

        for (int i = 0; i < count; i++) {
            String name = "ds_" + i;
            DataSourceProperties props = datasources.get(name);
            if (props == null) {
                throw new IllegalStateException("Missing datasource config for " + name);
            }
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

}
