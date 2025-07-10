package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.kafka.contracts.model.DepositRequestedPayload;
import com.advanced.kafka.contracts.model.WithdrawalRequestedPayload;
import com.advanced.transactionservice.mapper.KafkaPayloadMapper;
import com.advanced.transactionservice.mapper.PaymentRequestMapper;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
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
import java.util.Objects;
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

        BigDecimal fee = calculationFeeService.calculationWithdrawalFee();
        BigDecimal totalAmount = request.getAmount().add(fee).setScale(2, RoundingMode.HALF_EVEN);

        transactionValidation.validateDeposit(wallet);

        PaymentRequest payment = paymentRequestMapper.fromDeposit(
                request,
                UUID.fromString(Objects.requireNonNull(wallet.getWalletUid())),
                UUID.fromString(Objects.requireNonNull(wallet.getUserUid())),
                totalAmount,
                PENDING,
                DEPOSIT
        );

        paymentRequestRepository.save(payment);
        paymentRequestRepository.flush();

        depositRequestedProducer.send(kafkaPayloadMapper.toDepositRequestedPayload(payment));

        return getConfirmResponse(String.valueOf(payment.getUid()));
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
                UUID.fromString(Objects.requireNonNull(wallet.getWalletUid())),
                UUID.fromString(Objects.requireNonNull(wallet.getUserUid())),
                totalAmount,
                PENDING,
                WITHDRAWAL
        );

        paymentRequestRepository.save(payment);
        paymentRequestRepository.flush();

        withdrawalRequestedProducer.send(kafkaPayloadMapper.toWithdrawalRequestedPayload(payment));

        return getConfirmResponse(String.valueOf(payment.getUid()));
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
                UUID.fromString(Objects.requireNonNull(fromWallet.getWalletUid())),
                UUID.fromString(Objects.requireNonNull(toWallet.getWalletUid())),
                UUID.fromString(Objects.requireNonNull(fromWallet.getUserUid())),
                totalAmount,
                COMPLETED,
                TRANSFER
        );
        paymentRequestRepository.save(debitRequest);

        PaymentRequest creditRequest = paymentRequestMapper.fromTransfer(
                request,
                UUID.fromString(toWallet.getWalletUid()),
                UUID.fromString(fromWallet.getWalletUid()),
                UUID.fromString(Objects.requireNonNull(toWallet.getUserUid())),
                request.getAmount(),
                COMPLETED,
                TRANSFER
        );
        paymentRequestRepository.save(creditRequest);

        paymentRequestRepository.flush();

        return getConfirmResponse(debitRequest.getUid().toString());
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionStatusResponse getTransactionStatus(String transactionId) {
        UUID uid = UUID.fromString(transactionId);

        PaymentRequest payment = paymentRequestRepository.findById(uid)
                .orElseThrow(() -> new EntityNotFoundException("Transaction not found"));

        TransactionStatusResponse response = new TransactionStatusResponse();
        response.setTransactionUid(payment.getUid().toString());
        response.setStatus(payment.getStatus().name());
        response.setType(TransactionStatusResponse.TypeEnum.valueOf(payment.getType().getValue()));
        response.setAmount(payment.getAmount());
        response.setComment(payment.getComment());
        response.setFailureReason(payment.getFailureReason());
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
        response.setFee(fee);
        response.setAmount(amount);
        response.setTotalAmount(totalAmount);
        return response;
    }

    private static TransactionConfirmResponse getConfirmResponse(String payment) {
        TransactionConfirmResponse response = new TransactionConfirmResponse();
        response.setTransactionUid(payment);
        response.setStatus(CONFIRMED);
        return response;
    }

}
