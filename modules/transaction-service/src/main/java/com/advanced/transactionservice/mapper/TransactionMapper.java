package com.advanced.transactionservice.mapper;

import com.advanced.transactionservice.model.TransactionStatus;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransactionMapper {

    TransactionStatus map(com.advanced.kafka.contracts.model.TransactionStatus status);

    com.advanced.kafka.contracts.model.TransactionStatus map(TransactionStatus status);

}
