package com.advanced.individualsapi.controller;

import com.advanced.contract.api.TransactionRestControllerV1Api;
import com.advanced.contract.model.*;
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
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TransactionProxyControllerV1 {

    private final TransactionRestControllerV1Api transactionClient;

    @Timed
    @WithSpan
    @PostMapping("/{type}/init")
    public Mono<TransactionInitResponse> initTransaction(
            @PathVariable String type,
            @Valid @RequestBody Mono<InitRequest> requestMono
    ) {
        return requestMono
                .flatMap(req -> transactionClient.init(type, req));
    }

    @Timed
    @WithSpan
    @PostMapping("/{type}/confirm")
    public Mono<TransactionConfirmResponse> confirmTransaction(
            @PathVariable String type,
            @Valid @RequestBody Mono<ConfirmRequest> requestMono
    ) {
        return requestMono
                .flatMap(req -> transactionClient.confirm(type, req));
    }

    @Timed
    @WithSpan
    @GetMapping("/{transactionId}/status")
    public Mono<TransactionStatusResponse> getTransactionStatus(
            @PathVariable String transactionId
    ) {
        return transactionClient.getTransactionStatus(transactionId);
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
        String userUid = Optional.ofNullable(jwt.getClaimAsString("user_id"))
                .orElseThrow(() -> new IllegalArgumentException("user_id not found in token"));

        return transactionClient.searchTransactions(
                userUid,
                walletUid,
                type,
                status,
                dateFrom,
                dateTo,
                page,
                size
        );
    }

}
