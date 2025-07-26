package com.advanced.individualsapi.controller;

import com.advanced.contract.model.*;
import com.advanced.individualsapi.service.proxy.TransactionProxyService;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionProxyControllerV1 {

    private final TransactionProxyService proxyService;

    @Timed
    @WithSpan
    @PostMapping("/{type}/init")
    public Mono<TransactionInitResponse> initTransaction(
            @PathVariable String type,
            @RequestBody Mono<InitRequest> requestMono
    ) {
        return proxyService.init(type, requestMono);
    }

    @Timed
    @WithSpan
    @PostMapping("/{type}/confirm")
    public Mono<TransactionConfirmResponse> confirmTransaction(
            @PathVariable String type,
            @RequestBody Mono<ConfirmRequest> requestMono
    ) {
        return proxyService.confirm(type, requestMono);
    }

    @Timed
    @WithSpan
    @GetMapping("/{transactionId}/status")
    public Mono<TransactionStatusResponse> getTransactionStatus(@PathVariable String transactionId) {
        return proxyService.getTransactionStatus(transactionId);
    }

    @Timed
    @WithSpan
    @GetMapping
    public Flux<TransactionStatusResponse> searchTransactions(
            @RequestParam(required = false) String walletUid,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime dateTo,
            @RequestParam(required = false, defaultValue = "0") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return proxyService.searchTransactions(jwt, walletUid, type, status, dateFrom, dateTo, page, size);
    }

}
