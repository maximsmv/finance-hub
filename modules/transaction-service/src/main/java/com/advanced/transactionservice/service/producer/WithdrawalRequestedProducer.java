package com.advanced.transactionservice.service.producer;

import com.advanced.kafkacontracts.WithdrawalRequested;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WithdrawalRequestedProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaTopicsProperties topics;

    public void send(final WithdrawalRequested payload) {
        kafkaTemplate.send(topics.getWithdrawalRequested(), payload.getTransactionUid().toString(), payload);
    }

}
