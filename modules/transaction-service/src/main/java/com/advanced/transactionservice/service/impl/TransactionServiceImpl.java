package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.exception.TransactionConflictException;
import com.advanced.transactionservice.mapper.KafkaPayloadMapper;
import com.advanced.transactionservice.mapper.PaymentRequestMapper;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.service.TransactionService;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.producer.DepositRequestedProducer;
import com.advanced.transactionservice.service.producer.WithdrawalRequestedProducer;
import com.advanced.transactionservice.service.validation.TransactionValidation;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.postgresql.util.PSQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static com.advanced.contract.model.TransactionConfirmResponse.StatusEnum.CONFIRMED;
import static com.advanced.transactionservice.model.PaymentStatus.COMPLETED;
import static com.advanced.transactionservice.model.PaymentStatus.PENDING;
import static com.advanced.transactionservice.model.PaymentType.*;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final WalletService walletService;

    private final TransactionValidation transactionValidation;

    private final CalculationFeeService calculationFeeService;

    private final KafkaPayloadMapper kafkaPayloadMapper;

    private final PaymentRequestMapper paymentRequestMapper;

    private final PaymentRequestRepository paymentRequestRepository;

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

        BigDecimal fee = calculationFeeService.calculationDepositFee(request.getCurrency());
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateDeposit(wallet);

        PaymentRequest payment = paymentRequestMapper.fromDeposit(
                request,
                wallet.getWalletUid(),
                wallet.getUserUid(),
                totalAmount,
                PENDING,
                DEPOSIT
        );

        try {
            paymentRequestRepository.save(payment);
            paymentRequestRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ex.getRootCause() instanceof PSQLException psqlEx && "23505".equals(psqlEx.getSQLState())) {
                throw new TransactionConflictException();
            }
            throw ex;
        }

        depositRequestedProducer.send(kafkaPayloadMapper.toDepositRequestedPayload(payment));

        return getConfirmResponse(payment.getUid());
    }

    @Override
    @Transactional
    public TransactionConfirmResponse confirmWithdrawal(WithdrawalConfirmRequest request) {
        WalletResponse wallet = walletService.getWalletByUid(request.getWalletUid());

        BigDecimal fee = calculationFeeService.calculationWithdrawalFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateWithdrawal(wallet, totalAmount);

        PaymentRequest payment = paymentRequestMapper.fromWithdrawal(
                request,
                wallet.getWalletUid(),
                wallet.getUserUid(),
                totalAmount,
                PENDING,
                WITHDRAWAL
        );

        try {
            paymentRequestRepository.save(payment);
            paymentRequestRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (ex.getRootCause() instanceof PSQLException psqlEx && "23505".equals(psqlEx.getSQLState())) {
                throw new TransactionConflictException();
            }
            throw ex;
        }

        withdrawalRequestedProducer.send(kafkaPayloadMapper.toWithdrawalRequestedPayload(payment));

        return getConfirmResponse(payment.getUid());
    }

    @Override
    @Transactional
    public TransactionConfirmResponse confirmTransfer(TransferConfirmRequest request) {
        WalletResponse fromWallet = walletService.getWalletByUid(request.getWalletUid());
        WalletResponse toWallet = walletService.getWalletByUid(request.getTargetWalletUid());

        BigDecimal fee = calculationFeeService.calculationTransferFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        walletService.transfer(fromWallet.getWalletUid(), toWallet.getWalletUid(), totalAmount, request.getAmount());

        PaymentRequest debitRequest = paymentRequestMapper.fromTransfer(
                request,
                fromWallet.getWalletUid(),
                toWallet.getWalletUid(),
                fromWallet.getUserUid(),
                totalAmount,
                COMPLETED,
                TRANSFER
        );
        paymentRequestRepository.save(debitRequest);

        PaymentRequest creditRequest = paymentRequestMapper.fromTransfer(
                request,
                toWallet.getWalletUid(),
                fromWallet.getWalletUid(),
                toWallet.getUserUid(),
                request.getAmount(),
                COMPLETED,
                TRANSFER
        );
        paymentRequestRepository.save(creditRequest);

        paymentRequestRepository.flush();

        return getConfirmResponse(debitRequest.getUid());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        UUID transactionUid = UUID.fromString(transactionId);

        PaymentRequest payment = paymentRequestRepository.findByTransactionUid(transactionUid)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        return paymentRequestMapper.toTransactionStatusResponse(payment);
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
        response.setTransactionUid(UUID.randomUUID());
        response.setFee(fee);
        response.setAmount(amount);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private static TransactionConfirmResponse getConfirmResponse(UUID payment) {
        TransactionConfirmResponse response = new TransactionConfirmResponse();
        response.setTransactionUid(payment);
        response.setStatus(CONFIRMED);
        return response;
    }

}
