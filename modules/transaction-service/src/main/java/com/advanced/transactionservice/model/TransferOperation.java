package com.advanced.transactionservice.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;


@Entity
@Table(name = "transfer_operations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"id"})
public class TransferOperation {
    @Id
    @GeneratedValue
    private UUID uid;

    @Column(nullable = false)
    private UUID transactionUid;

    @OneToMany(mappedBy = "transferOperation", cascade = CascadeType.ALL)
    private List<PaymentRequest> paymentRequests;
}
