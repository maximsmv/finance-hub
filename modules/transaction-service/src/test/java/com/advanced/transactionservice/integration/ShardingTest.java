package com.advanced.transactionservice.integration;

import com.advanced.transactionservice.AbstractIntegrationTest;
import com.advanced.transactionservice.configuration.ShardingSphereConfiguration;
import com.advanced.transactionservice.model.Wallet;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.model.WalletType;
import com.advanced.transactionservice.repository.WalletRepository;
import com.advanced.transactionservice.repository.WalletTypeRepository;
import com.advanced.transactionservice.utils.WalletUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@Testcontainers
public class ShardingTest extends AbstractIntegrationTest {

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTypeRepository walletTypeRepository;

    @Autowired
    private ShardingSphereConfiguration shardingSphereConfiguration;

    private final UUID userInShard0 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private final UUID userInShard1 = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void testShardingDistribution() throws SQLException {
        walletRepository.deleteAll();

        Wallet wallet0 = new Wallet();
        wallet0.setUserUid(userInShard0);
        wallet0.setStatus(WalletStatus.ACTIVE);
        wallet0.setName("wallet0");
        wallet0.setBalance(BigDecimal.ZERO);
        wallet0.setWalletType(walletTypeRepository.findById(WalletUtils.WALLET_TYPE_UID).orElseThrow());

        Wallet wallet1 = new Wallet();
        wallet1.setUserUid(userInShard1);
        wallet1.setStatus(WalletStatus.ACTIVE);
        wallet1.setName("wallet0");
        wallet1.setBalance(BigDecimal.ZERO);
        wallet1.setWalletType(walletTypeRepository.findById(WalletUtils.WALLET_TYPE_UID).orElseThrow());

        walletRepository.save(wallet0);
        walletRepository.save(wallet1);

        Map<String, ShardingSphereConfiguration.DataSourceProperties> datasources = shardingSphereConfiguration.getDatasources();

        for (int i = 0; i < datasources.size(); i++) {
            String shardName = "ds_" + i;
            var props = datasources.get(shardName);

            try (Connection conn = DriverManager.getConnection(props.getJdbcUrl(), props.getUsername(), props.getPassword());
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM wallets")) {

                ResultSet rs = stmt.executeQuery();
                List<UUID> foundUserUids = new ArrayList<>();
                while (rs.next()) {
                    foundUserUids.add(UUID.fromString(rs.getString("user_uid")));
                }

                System.out.println("Shard: " + shardName + " -> " + foundUserUids);
                for (UUID uid : foundUserUids) {
                    int expectedShard = computeShard(uid, datasources.size());
                    Assertions.assertEquals(i, expectedShard, "user " + uid + " должен быть в ds_" + expectedShard);
                }
            }
        }
    }

    private int computeShard(UUID userUid, int count) {
        return Math.floorMod(userUid.hashCode(), count);
    }

}
