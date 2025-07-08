package com.advanced.transactionservice.mapper;

import com.advanced.transactionservice.model.FailureReason;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface FailureReasonMapper {

    FailureReason map(com.advanced.kafka.contracts.model.FailureReason status);

    com.advanced.kafka.contracts.model.FailureReason map(FailureReason status);

}
