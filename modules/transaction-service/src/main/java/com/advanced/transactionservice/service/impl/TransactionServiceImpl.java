package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.model.Currency;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.service.TransactionService;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.validation.TransactionValidate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletService walletService;

    private final TransactionValidate transactionValidate;

    private final CalculationFeeService calculationFeeService;

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initDeposit(DepositInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        transactionValidate.validateInitDepositRequest(wallet);

        return getTransactionInitResponse(
                calculationFeeService.calculationDepositFee(Currency.valueOf(wallet.getCurrency())),
                request.getAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initWithdrawal(WithdrawalInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        BigDecimal fee = calculationFeeService.calculationWithdrawalFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidate.validateInitWithdrawalRequest(wallet, totalAmount);

        return getTransactionInitResponse(
                fee,
                request.getAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initTransfer(TransferInitRequest request) {
        WalletResponse fromWalletUid = walletService.getWalletByUid(request.getFromWalletUid());
        WalletResponse toWalletUid = walletService.getWalletByUid(request.getToWalletUid());

        BigDecimal fee = calculationFeeService.calculationTransferFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidate.validateInitTransferRequest(fromWalletUid, toWalletUid, totalAmount);

        return getTransactionInitResponse(
                fee,
                request.getAmount()
        );
    }

    @Override
    public TransactionConfirmResponse confirmDeposit(DepositConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionConfirmResponse confirmWithdrawal(WithdrawalConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionConfirmResponse confirmTransfer(TransferConfirmRequest request) {
        return null;
    }

    @Override
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        return null;
    }

    @Override
    public List<TransactionStatusResponse> searchTransactions(
            String userUid,
            String walletUid,
            String type,
            String status,
            LocalDateTime dateFrom,
            LocalDateTime dateTo,
            int page,
            int size
    ) {
        return List.of();
    }

    private TransactionInitResponse getTransactionInitResponse(BigDecimal fee, BigDecimal request) {
        TransactionInitResponse response = new TransactionInitResponse();
        response.setFee(fee);
        response.setAmount(request);
        response.setTotalAmount(request.add(response.getFee()));
        return response;
    }

}
