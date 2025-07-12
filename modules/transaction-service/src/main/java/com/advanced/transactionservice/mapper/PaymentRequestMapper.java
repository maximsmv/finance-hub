package com.advanced.transactionservice.mapper;

import com.advanced.contract.model.DepositConfirmRequest;
import com.advanced.contract.model.TransactionStatusResponse;
import com.advanced.contract.model.TransferConfirmRequest;
import com.advanced.contract.model.WithdrawalConfirmRequest;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.util.UUID;

@Mapper(componentModel = "spring", imports = {UUID.class, PaymentStatus.class, PaymentType.class})
public interface PaymentRequestMapper {

    @Mapping(source = "uid", target = "transactionUid")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "type", target = "type")
    @Mapping(source = "amount", target = "amount")
    TransactionStatusResponse toTransactionStatusResponse(PaymentRequest entity);

    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "targetWalletUid", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "walletUid", source = "walletUid")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "fee", source = "fee")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "comment", expression = "java(request.getComment())")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "userUid", source = "userUid")
    PaymentRequest fromDeposit(
            DepositConfirmRequest request,
            UUID walletUid,
            UUID userUid,
            BigDecimal amount,
            BigDecimal fee,
            PaymentStatus status,
            PaymentType type
    );

    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "targetWalletUid", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "walletUid", source = "walletUid")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "fee", source = "fee")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "comment", expression = "java(request.getComment())")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "userUid", source = "userUid")
    PaymentRequest fromWithdrawal(
            WithdrawalConfirmRequest request,
            UUID walletUid,
            UUID userUid,
            BigDecimal amount,
            BigDecimal fee,
            PaymentStatus status,
            PaymentType type
    );

    @Mapping(target = "uid", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "processedAt", ignore = true)
    @Mapping(target = "expiresAt", ignore = true)
    @Mapping(target = "transactionUid", ignore = true)
    @Mapping(target = "walletUid", source = "fromWalletUid")
    @Mapping(target = "targetWalletUid", source = "toWalletUid")
    @Mapping(target = "amount", source = "amount")
    @Mapping(target = "fee", source = "fee")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "comment", expression = "java(request.getComment())")
    @Mapping(target = "type", source = "type")
    @Mapping(target = "userUid", source = "userUid")
    PaymentRequest fromTransfer(
            TransferConfirmRequest request,
            UUID fromWalletUid,
            UUID toWalletUid,
            UUID userUid,
            BigDecimal amount,
            BigDecimal fee,
            PaymentStatus status,
            PaymentType type
    );

}
