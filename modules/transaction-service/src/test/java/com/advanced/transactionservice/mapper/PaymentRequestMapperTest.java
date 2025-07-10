package com.advanced.transactionservice.mapper;

import com.advanced.contract.model.DepositConfirmRequest;
import com.advanced.contract.model.TransactionStatusResponse;
import com.advanced.contract.model.TransferConfirmRequest;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentRequestMapperTest {

    private final PaymentRequestMapper mapper = Mappers.getMapper(PaymentRequestMapper.class);

    @Test
    void fromDeposit_shouldMapCorrectly() {
        var request = new DepositConfirmRequest();
        request.setComment("Deposit to main");
        UUID walletUid = UUID.randomUUID();
        UUID userUid = UUID.randomUUID();

        var result = mapper.fromDeposit(
                request,
                walletUid,
                userUid,
                new BigDecimal("100.00"),
                PaymentStatus.PENDING,
                PaymentType.DEPOSIT
        );

        assertEquals(walletUid, result.getWalletUid());
        assertEquals(userUid, result.getUserUid());
        assertEquals("100.00", result.getAmount().toPlainString());
        assertEquals(PaymentStatus.PENDING, result.getStatus());
        assertEquals(PaymentType.DEPOSIT, result.getType());
        assertEquals("Deposit to main", result.getComment());
    }

    @Test
    void fromTransfer_shouldMapWithTargetWallet() {
        var request = new TransferConfirmRequest();
        request.setComment("Sending funds");

        UUID fromWallet = UUID.randomUUID();
        UUID toWallet = UUID.randomUUID();
        UUID user = UUID.randomUUID();

        var result = mapper.fromTransfer(
                request,
                fromWallet,
                toWallet,
                user,
                new BigDecimal("250.00"),
                PaymentStatus.COMPLETED,
                PaymentType.TRANSFER
        );

        assertEquals(fromWallet, result.getWalletUid());
        assertEquals(toWallet, result.getTargetWalletUid());
        assertEquals(user, result.getUserUid());
        assertEquals("250.00", result.getAmount().toPlainString());
        assertEquals("Sending funds", result.getComment());
    }

    @Test
    void toTransactionStatusResponse_shouldMapBasicFields() {
        var entity = new PaymentRequest();
        UUID id = UUID.randomUUID();
        entity.setUid(id);
        entity.setStatus(PaymentStatus.COMPLETED);
        entity.setType(PaymentType.TRANSFER);
        entity.setAmount(new BigDecimal("77.77"));

        var response = mapper.toTransactionStatusResponse(entity);

        assertEquals(id.toString(), response.getTransactionUid());
        assertEquals(PaymentStatus.COMPLETED.getValue(), response.getStatus());
        assertEquals(TransactionStatusResponse.TypeEnum.TRANSFER, response.getType());
        assertEquals(new BigDecimal("77.77"), response.getAmount());
    }

}