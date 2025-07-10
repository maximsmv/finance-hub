package com.advanced.transactionservice.service.producer;

import com.advanced.kafka.contracts.model.DepositRequestedPayload;
import com.advanced.kafka.contracts.model.WithdrawalRequestedPayload;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalRequestedProducer {

    private final KafkaTemplate<String, WithdrawalRequestedPayload> kafkaTemplate;

    public void send(final WithdrawalRequestedPayload payload) {
        kafkaTemplate.send("withdrawal-requested", payload.getTransactionId(), payload);
    }

}
