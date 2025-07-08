package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.model.Currency;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.service.TransactionService;
import com.advanced.transactionservice.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletService walletService;

    private final CalculationFeeService calculationFeeService;

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initDeposit(DepositInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        TransactionInitResponse response = getTransactionInitResponse(wallet);
        if (Objects.nonNull(response)) {
            return response;
        }

        return getTransactionInitResponse(
                calculationFeeService.calculationDepositFee(Currency.valueOf(wallet.getCurrency())),
                request.getAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initWithdrawal(WithdrawalInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        TransactionInitResponse response = getTransactionInitResponse(wallet);
        if (Objects.nonNull(response)) {
            return response;
        }

        //TODO: Проверка баланса

        return getTransactionInitResponse(
                calculationFeeService.calculationWithdrawalFee(),
                request.getAmount()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initTransfer(TransferInitRequest request) {
        WalletResponse fromWalletUid = walletService.getWalletByUid(request.getFromWalletUid());
        WalletResponse toWalletUid = walletService.getWalletByUid(request.getToWalletUid());

        TransactionInitResponse response = getTransactionInitResponse(fromWalletUid, toWalletUid);
        if (Objects.nonNull(response)) {
            return response;
        }

        //TODO: Сделать проверку баланса

        return getTransactionInitResponse(
                calculationFeeService.calculationTransferFee(),
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
    public List<TransactionStatusResponse> searchTransactions(String userUid, String walletUid, String type, String status, LocalDateTime dateFrom, LocalDateTime dateTo, int page, int size) {
        return List.of();
    }

    private TransactionInitResponse getTransactionInitResponse(BigDecimal fee, BigDecimal request) {
        TransactionInitResponse response = new TransactionInitResponse();
        response.setAvailable(true);
        response.setFee(fee);
        response.setAmount(request);
        response.setTotalAmount(request.add(response.getFee()));
        return response;
    }

    private static TransactionInitResponse getTransactionInitResponse(WalletResponse wallet) {
        if (!WalletStatus.ACTIVE.getValue().equalsIgnoreCase(wallet.getStatus())) {
            TransactionInitResponse response = new TransactionInitResponse();
            response.setAvailable(false);
            response.setReason("Wallet is not active");
            return response;
        }
        return null;
    }

    private static TransactionInitResponse getTransactionInitResponse(
            WalletResponse toWallet,
            WalletResponse fromWallet
    ) {
        if (!WalletStatus.ACTIVE.getValue().equalsIgnoreCase(toWallet.getStatus())
                || !WalletStatus.ACTIVE.getValue().equalsIgnoreCase(fromWallet.getStatus())) {
            TransactionInitResponse response = new TransactionInitResponse();
            response.setAvailable(false);
            response.setReason("One of Wallets is not active");
            return response;
        }
        return null;
    }

}
