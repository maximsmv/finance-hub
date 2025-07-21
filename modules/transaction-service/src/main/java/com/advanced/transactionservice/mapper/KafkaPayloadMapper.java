package com.advanced.transactionservice.mapper;

import com.advanced.kafkacontracts.DepositRequested;
import com.advanced.kafkacontracts.WithdrawalRequested;
import com.advanced.transactionservice.model.Transaction;

public class KafkaPayloadMapper {

    public static DepositRequested toDepositRequestedPayload(Transaction payment) {
        return new DepositRequested(
                payment.getUid(),
                payment.getUserUid(),
                payment.getWalletUid(),
                payment.getAmount(),
                payment.getCurrency().toString(),
                payment.getCreatedAt().toInstant()
        );
    };

    public static WithdrawalRequested toWithdrawalRequestedPayload(Transaction payment, String destination) {
        return new WithdrawalRequested(
                payment.getUid(),
                payment.getUserUid(),
                payment.getWalletUid(),
                payment.getAmount(),
                payment.getCurrency().toString(),
                destination,
                payment.getCreatedAt().toInstant()
        );
    };

}
