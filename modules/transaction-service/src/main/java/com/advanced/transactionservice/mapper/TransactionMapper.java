package com.advanced.transactionservice.mapper;

import com.advanced.transactionservice.model.PaymentStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    PaymentStatus map(com.advanced.kafka.contracts.model.TransactionStatus status);

    com.advanced.kafka.contracts.model.TransactionStatus map(PaymentStatus status);

}
