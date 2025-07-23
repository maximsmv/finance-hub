package com.advanced.transactionservice.controller;

import com.advanced.contract.model.*;
import com.advanced.transactionservice.model.PaymentStatus;
import com.advanced.transactionservice.model.PaymentType;
import com.advanced.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/transactions")
public class TransactionRestControllerV1 {

    private final TransactionService transactionService;

    @PostMapping("/deposit/init")
    public TransactionInitResponse init(@Valid @RequestBody DepositInitRequest request) {
        return transactionService.initDeposit(request);
    }

    @PostMapping("/withdrawal/init")
    public TransactionInitResponse init(@Valid @RequestBody WithdrawalInitRequest request) {
        return transactionService.initWithdrawal(request);
    }

    @PostMapping("/transfer/init")
    public TransactionInitResponse init(@Valid @RequestBody TransferInitRequest request) {
        return transactionService.initTransfer(request);
    }

    @PostMapping("/deposit/confirm")
    public TransactionConfirmResponse confirm(@Valid @RequestBody DepositConfirmRequest request) {
        return transactionService.confirmDeposit(request);
    }

    @PostMapping("/withdrawal/confirm")
    public TransactionConfirmResponse confirm(@Valid @RequestBody WithdrawalConfirmRequest request) {
        return transactionService.confirmWithdrawal(request);
    }

    @PostMapping("/transfer/confirm")
    public TransactionConfirmResponse confirm(@Valid @RequestBody TransferConfirmRequest request) {
        return transactionService.confirmTransfer(request);
    }

    @GetMapping("/{transactionId}/status")
    public TransactionStatusResponse getTransactionStatus(@PathVariable String transactionId) {
        return transactionService.getTransactionStatus(transactionId);
    }

    @GetMapping
    public List<TransactionStatusResponse> searchTransactions(
            @RequestParam(required = false) String userUid,
            @RequestParam(required = false) String walletUid,
            @RequestParam(required = false) PaymentType type,
            @RequestParam(required = false) PaymentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return transactionService.searchTransactions(userUid, walletUid, type, status, dateFrom, dateTo, page, size);
    }

}
