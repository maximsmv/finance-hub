package com.advanced.transactionservice.service.producer;

import com.advanced.kafkacontracts.DepositRequested;
import com.advanced.transactionservice.configuration.KafkaTopicsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DepositRequestedProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    private final KafkaTopicsProperties topics;

    public void send(final DepositRequested payload) {
        kafkaTemplate.send(topics.getDepositRequested(), payload.getTransactionUid().toString(), payload);
    }

}
