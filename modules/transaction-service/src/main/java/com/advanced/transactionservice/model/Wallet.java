package com.advanced.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"uid"})
public class Wallet {

    @Id
    @GeneratedValue
    private UUID uid;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    private OffsetDateTime modifiedAt;

    @Column(nullable = false, length = 32)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_type_uid", nullable = false)
    private WalletType walletType;

    @Column(nullable = false)
    private UUID userUid;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletStatus status;

    @Column(nullable = false)
    private BigDecimal balance;

    private OffsetDateTime archivedAt;

}
