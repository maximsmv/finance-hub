package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.mapper.KafkaPayloadMapper;
import com.advanced.transactionservice.mapper.TransactionMapper;
import com.advanced.transactionservice.model.Transaction;
import com.advanced.transactionservice.repository.TransactionRepository;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.service.TransactionService;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.producer.DepositRequestedProducer;
import com.advanced.transactionservice.service.producer.WithdrawalRequestedProducer;
import com.advanced.transactionservice.service.validation.TransactionValidation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.advanced.contract.model.TransactionConfirmResponse.StatusEnum.PENDING;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletService walletService;

    private final TransactionValidation transactionValidation;

    private final CalculationFeeService calculationFeeService;

    private final TransactionRepository transactionRepository;

    private final DepositRequestedProducer depositRequestedProducer;

    private final WithdrawalRequestedProducer withdrawalRequestedProducer;

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initDeposit(DepositInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        BigDecimal fee = calculationFeeService.calculationDepositFee(request.getCurrency());
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateDeposit(wallet);

        return getTransactionInitResponse(
                fee,
                request.getAmount(),
                totalAmount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initWithdrawal(WithdrawalInitRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        BigDecimal fee = calculationFeeService.calculationWithdrawalFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateWithdrawal(wallet, totalAmount);

        return getTransactionInitResponse(
                fee,
                request.getAmount(),
                totalAmount
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionInitResponse initTransfer(TransferInitRequest request) {
        WalletResponse fromWalletUid = walletService.getWalletByUid(request.getFromWalletUid());
        WalletResponse toWalletUid = walletService.getWalletByUid(request.getToWalletUid());

        BigDecimal fee = calculationFeeService.calculationTransferFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateTransfer(fromWalletUid, toWalletUid, totalAmount);

        return getTransactionInitResponse(
                fee,
                request.getAmount(),
                totalAmount
        );
    }

    @Override
    @Transactional
    public TransactionConfirmResponse confirmDeposit(DepositConfirmRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        Transaction transaction = TransactionMapper.fromDeposit(request, wallet);

        transactionRepository.saveAndFlush(transaction);
        depositRequestedProducer.send(KafkaPayloadMapper.toDepositRequestedPayload(transaction));

        return getConfirmResponse(transaction.getUid());
    }

    @Override
    @Transactional
    public TransactionConfirmResponse confirmWithdrawal(WithdrawalConfirmRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        transactionValidation.validateWithdrawal(wallet, request.getAmount());

        walletService.debit(wallet.getWalletUid(), wallet.getUserUid(), request.getAmount());

        Transaction transaction = TransactionMapper.fromWithdrawal(request, wallet);
        transactionRepository.saveAndFlush(transaction);

        withdrawalRequestedProducer.send(KafkaPayloadMapper.toWithdrawalRequestedPayload(transaction, request.getDestination()));

        return getConfirmResponse(transaction.getUid());
    }

    @Override
    @Transactional
    public TransactionConfirmResponse confirmTransfer(TransferConfirmRequest request) {
        WalletResponse fromWallet = walletService.getWalletByUid(request.getWalletUid());
        WalletResponse toWallet = walletService.getWalletByUid(request.getTargetWalletUid());

        walletService.transfer(fromWallet.getWalletUid(), fromWallet.getUserUid(), toWallet.getWalletUid(), toWallet.getUserUid(), request.getAmount().add(request.getFee()), request.getAmount());

        Transaction transaction = TransactionMapper.fromTransfer(
                request,
                fromWallet,
                toWallet.getWalletUid()
        );
        transactionRepository.save(transaction);

        return getConfirmResponse(transaction.getUid(), TransactionConfirmResponse.StatusEnum.COMPLETED);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        UUID transactionUid = UUID.fromString(transactionId);

        Transaction payment = transactionRepository.findById(transactionUid)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        return TransactionMapper.toTransactionStatusResponse(payment);
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

    private TransactionInitResponse getTransactionInitResponse(BigDecimal fee, BigDecimal amount, BigDecimal totalAmount) {
        TransactionInitResponse response = new TransactionInitResponse();
        response.setFee(fee);
        response.setAmount(amount);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private static TransactionConfirmResponse getConfirmResponse(UUID transactionUid) {
        TransactionConfirmResponse response = new TransactionConfirmResponse();
        response.setTransactionUid(transactionUid);
        response.setStatus(PENDING);
        return response;
    }

    private static TransactionConfirmResponse getConfirmResponse(UUID transactionUid, TransactionConfirmResponse.StatusEnum statusEnum) {
        TransactionConfirmResponse response = new TransactionConfirmResponse();
        response.setTransactionUid(transactionUid);
        response.setStatus(statusEnum);
        return response;
    }

}
