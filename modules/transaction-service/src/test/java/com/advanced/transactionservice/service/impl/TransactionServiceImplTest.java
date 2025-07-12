package com.advanced.transactionservice.service.impl;

import com.advanced.contract.model.*;
import com.advanced.kafka.contracts.model.DepositRequestedPayload;
import com.advanced.kafka.contracts.model.WithdrawalRequestedPayload;
import com.advanced.transactionservice.exception.TransferSameWalletsException;
import com.advanced.transactionservice.exception.WalletStatusException;
import com.advanced.transactionservice.mapper.KafkaPayloadMapper;
import com.advanced.transactionservice.mapper.PaymentRequestMapper;
import com.advanced.transactionservice.model.PaymentRequest;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.model.WalletStatus;
import com.advanced.transactionservice.repository.PaymentRequestRepository;
import com.advanced.transactionservice.repository.TransferOperationRepository;
import com.advanced.transactionservice.service.CalculationFeeService;
import com.advanced.transactionservice.service.WalletService;
import com.advanced.transactionservice.service.producer.DepositRequestedProducer;
import com.advanced.transactionservice.service.producer.WithdrawalRequestedProducer;
import com.advanced.transactionservice.service.validation.TransactionValidation;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

import static com.advanced.transactionservice.model.PaymentStatus.COMPLETED;
import static com.advanced.transactionservice.model.PaymentStatus.PENDING;
import static com.advanced.transactionservice.model.PaymentType.WITHDRAWAL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock
    private WalletService walletService;

    @Mock
    private TransactionValidation transactionValidation;

    @Mock
    private CalculationFeeService calculationFeeService;

    @Mock
    private KafkaPayloadMapper kafkaPayloadMapper;

    @Mock
    private PaymentRequestMapper paymentRequestMapper;

    @Mock
    private PaymentRequestRepository paymentRequestRepository;

    @Mock
    private DepositRequestedProducer depositRequestedProducer;

    @Mock
    private WithdrawalRequestedProducer withdrawalRequestedProducer;

    @Mock
    private TransferOperationRepository transferOperationRepository;

    @InjectMocks
    private TransactionServiceImpl transactionService;

    @Test
    void initDeposit_shouldCalculateFeeAndReturnResponse() {
        UUID walletUid = UUID.randomUUID();
        var request = new DepositInitRequest();
        request.setWalletUid(walletUid);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("USD");

        var wallet = new WalletResponse();
        wallet.setWalletUid(walletUid);

        Mockito.when(walletService.getWalletByUid(walletUid)).thenReturn(wallet);
        Mockito.when(calculationFeeService.calculationDepositFee("USD")).thenReturn(BigDecimal.valueOf(2));

        var response = transactionService.initDeposit(request);

        assertEquals(BigDecimal.valueOf(2), response.getFee());
        assertEquals(BigDecimal.valueOf(100), response.getAmount());
        assertEquals(BigDecimal.valueOf(102).setScale(2, RoundingMode.HALF_EVEN), response.getTotalAmount());
    }

    @Test
    void initWithdrawal_shouldFailIfWalletBlocked() {
        UUID walletUid = UUID.randomUUID();
        var request = new WithdrawalInitRequest();
        request.setWalletUid(walletUid);
        request.setAmount(BigDecimal.valueOf(50));

        var wallet = new WalletResponse();
        wallet.setWalletUid(walletUid);
        wallet.setStatus(WalletStatus.BLOCKED.getValue());

        Mockito.when(walletService.getWalletByUid(walletUid)).thenReturn(wallet);
        Mockito.when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.valueOf(1));
        Mockito.doThrow(new WalletStatusException(WalletStatus.BLOCKED, walletUid))
                .when(transactionValidation)
                .validateWithdrawal(eq(wallet), any());
        assertThrows(WalletStatusException.class, () -> transactionService.initWithdrawal(request));
    }

    @Test
    void initTransfer_shouldReturnCorrectResponse() {
        UUID fromUid = UUID.randomUUID();
        UUID toUid = UUID.randomUUID();
        UUID userUid = UUID.randomUUID();

        var request = new TransferInitRequest();
        request.setFromWalletUid(fromUid);
        request.setToWalletUid(toUid);
        request.setAmount(BigDecimal.valueOf(100));

        var fromWallet = new WalletResponse();
        fromWallet.setWalletUid(fromUid);
        fromWallet.setUserUid(userUid);
        fromWallet.setBalance(BigDecimal.valueOf(500));
        fromWallet.setStatus(WalletStatus.ACTIVE.getValue());

        var toWallet = new WalletResponse();
        toWallet.setWalletUid(toUid);
        toWallet.setUserUid(userUid);
        toWallet.setBalance(BigDecimal.valueOf(200));
        toWallet.setStatus(WalletStatus.ACTIVE.getValue());

        Mockito.when(walletService.getWalletByUid(fromUid)).thenReturn(fromWallet);
        Mockito.when(walletService.getWalletByUid(toUid)).thenReturn(toWallet);
        Mockito.when(calculationFeeService.calculationTransferFee()).thenReturn(BigDecimal.valueOf(5));

        var result = transactionService.initTransfer(request);

        assertEquals(BigDecimal.valueOf(100), result.getAmount());
        assertEquals(BigDecimal.valueOf(5), result.getFee());
        assertEquals(BigDecimal.valueOf(105.00).setScale(2, RoundingMode.HALF_EVEN), result.getTotalAmount());

        Mockito.verify(transactionValidation).validateTransfer(fromWallet, toWallet, BigDecimal.valueOf(105.00).setScale(2, RoundingMode.HALF_EVEN));
    }

    @Test
    void confirmDeposit_shouldSavePaymentAndSendKafkaEvent() {
        UUID walletUid = UUID.randomUUID();
        UUID userUid = UUID.randomUUID();

        var request = new DepositConfirmRequest();
        request.setWalletUid(walletUid);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("RUB");
        request.setComment("comment");

        var wallet = new WalletResponse();
        wallet.setWalletUid(walletUid);
        wallet.setBalance(BigDecimal.valueOf(100));
        wallet.setCurrency("RUB");
        wallet.setUserUid(userUid);

        var payment = new PaymentRequest();
        payment.setUid(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(102));
        payment.setWalletUid(walletUid);
        payment.setUserUid(userUid);
        payment.setComment("comment");
        payment.setStatus(PENDING);
        payment.setType(PaymentType.DEPOSIT);

        Mockito.when(walletService.getWalletByUid(walletUid)).thenReturn(wallet);
        Mockito.when(calculationFeeService.calculationDepositFee("RUB")).thenReturn(BigDecimal.valueOf(2));
        Mockito.when(paymentRequestMapper.fromDeposit(
                any(), any(), any(), any(), any(), any(), any())
        ).thenReturn(payment);
        Mockito.when(kafkaPayloadMapper.toDepositRequestedPayload(any())).thenReturn(new DepositRequestedPayload());

        var response = transactionService.confirmDeposit(request);

        Mockito.verify(paymentRequestRepository).save(payment);
        Mockito.verify(depositRequestedProducer).send(any(DepositRequestedPayload.class));
        assertEquals(payment.getTransactionUid(), response.getTransactionUid());
        assertEquals(TransactionConfirmResponse.StatusEnum.CONFIRMED, response.getStatus());
    }

    @Test
    void confirmWithdrawal_shouldSaveRequestAndSendKafka() {
        UUID walletUid = UUID.randomUUID();
        UUID userUid = UUID.randomUUID();

        var request = new WithdrawalConfirmRequest();
        request.setWalletUid(walletUid);
        request.setAmount(BigDecimal.valueOf(100));
        request.setCurrency("USD");

        var wallet = new WalletResponse();
        wallet.setWalletUid(walletUid);
        wallet.setUserUid(userUid);
        wallet.setBalance(BigDecimal.valueOf(200));
        wallet.setStatus(WalletStatus.ACTIVE.getValue());

        var payment = new PaymentRequest();
        payment.setUid(UUID.randomUUID());
        payment.setAmount(BigDecimal.valueOf(105));
        payment.setStatus(PENDING);
        payment.setType(WITHDRAWAL);
        payment.setTransactionUid(UUID.randomUUID());

        Mockito.when(walletService.getWalletByUid(walletUid)).thenReturn(wallet);
        Mockito.when(calculationFeeService.calculationWithdrawalFee()).thenReturn(BigDecimal.valueOf(5));
        Mockito.when(paymentRequestMapper.fromWithdrawal(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(payment);

        var payload = new WithdrawalRequestedPayload();
        Mockito.when(kafkaPayloadMapper.toWithdrawalRequestedPayload(payment)).thenReturn(payload);

        var result = transactionService.confirmWithdrawal(request);

        Mockito.verify(paymentRequestRepository).saveAndFlush(payment);
        Mockito.verify(withdrawalRequestedProducer).send(payload);

        assertEquals(payment.getTransactionUid(), result.getTransactionUid());
        assertEquals(TransactionConfirmResponse.StatusEnum.CONFIRMED, result.getStatus());
    }

    @Test
    void confirmTransfer_shouldCreateTwoTransactions() {
        UUID toWalletUid = UUID.randomUUID();
        UUID fromWalletUid = UUID.randomUUID();
        UUID userUid = UUID.randomUUID();

        var request = new TransferConfirmRequest();
        request.setWalletUid(fromWalletUid);
        request.setTargetWalletUid(toWalletUid);
        request.setAmount(BigDecimal.valueOf(50));
        request.setCurrency("RUB");
        request.setComment("comment");

        var from = new WalletResponse();
        from.setWalletUid(fromWalletUid);
        from.setUserUid(userUid);
        from.setBalance(BigDecimal.valueOf(100));
        from.setStatus(WalletStatus.ACTIVE.getValue());

        var to = new WalletResponse();
        to.setWalletUid(toWalletUid);
        to.setUserUid(userUid);
        to.setBalance(BigDecimal.valueOf(100));
        to.setStatus(WalletStatus.ACTIVE.getValue());

        var debit = new PaymentRequest();
        debit.setUid(UUID.randomUUID());

        var credit = new PaymentRequest();

        Mockito.when(walletService.getWalletByUid(fromWalletUid)).thenReturn(from);
        Mockito.when(walletService.getWalletByUid(toWalletUid)).thenReturn(to);
        Mockito.when(calculationFeeService.calculationTransferFee()).thenReturn(BigDecimal.valueOf(5));
        Mockito.when(paymentRequestMapper.fromTransfer(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(debit).thenReturn(credit);

        var result = transactionService.confirmTransfer(request);

        Mockito.verify(walletService).transfer(fromWalletUid, toWalletUid, BigDecimal.valueOf(55).setScale(2, RoundingMode.HALF_EVEN), BigDecimal.valueOf(50));
        Mockito.verify(paymentRequestRepository, Mockito.times(2)).save(any());
        Mockito.verify(transferOperationRepository, Mockito.times(1)).saveAndFlush(any());
    }

    @Test
    void confirmTransfer_shouldThrowIfWalletsAreSame() {
        var walletUid = UUID.randomUUID();

        var request = new TransferConfirmRequest();
        request.setWalletUid(walletUid);
        request.setTargetWalletUid(walletUid);
        request.setAmount(BigDecimal.valueOf(100));

        var wallet = new WalletResponse();
        wallet.setWalletUid(walletUid);
        wallet.setUserUid(UUID.randomUUID());
        wallet.setBalance(BigDecimal.valueOf(500));
        wallet.setStatus(WalletStatus.ACTIVE.getValue());

        Mockito.when(walletService.getWalletByUid(walletUid)).thenReturn(wallet);
        Mockito.when(calculationFeeService.calculationTransferFee()).thenReturn(BigDecimal.valueOf(5));

        Mockito.doThrow(new TransferSameWalletsException())
                .when(walletService).transfer(any(), any(), any(), any());

        assertThrows(TransferSameWalletsException.class, () -> transactionService.confirmTransfer(request));
    }

    @Test
    void getTransactionStatus_shouldReturnMappedResponse() {
        UUID transactionId = UUID.randomUUID();
        PaymentRequest entity = new PaymentRequest();
        entity.setUid(UUID.randomUUID());
        entity.setType(PaymentType.TRANSFER);
        entity.setStatus(COMPLETED);
        entity.setTransactionUid(transactionId);

        Mockito.when(paymentRequestRepository.findByTransactionUid(transactionId)).thenReturn(Optional.of(entity));
        Mockito.when(paymentRequestMapper.toTransactionStatusResponse(entity))
                .thenReturn(new TransactionStatusResponse().transactionUid(transactionId).status(COMPLETED.getValue()));

        var result = transactionService.getTransactionStatus(transactionId.toString());

        assertEquals(COMPLETED.getValue(), result.getStatus());
        assertEquals(transactionId, result.getTransactionUid());
    }

    @Test
    void getTransactionStatus_shouldThrowIfNotFound() {
        UUID uid = UUID.randomUUID();
        Mockito.when(paymentRequestRepository.findByTransactionUid(uid)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> transactionService.getTransactionStatus(uid.toString()));
    }

}