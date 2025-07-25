package com.advanced.individualsapi.controller;

import com.advanced.contract.api.WalletRestControllerV1Api;
import com.advanced.contract.model.WalletResponse;
import com.advanced.individualsapi.dto.CreateWalletRequest;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/wallets")
@RequiredArgsConstructor
public class WalletProxyControllerV1 {

    private final WalletRestControllerV1Api transactionClient;

    @Timed
    @WithSpan
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<WalletResponse> getWalletsByCurrentUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return extractUserUid(jwt)
                .flatMapMany(userUid -> transactionClient.getWalletsByUser(userUid.toString()));
    }

    @Timed
    @WithSpan
    @GetMapping("/{walletUid}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<WalletResponse> getWalletByUid(
            @PathVariable UUID walletUid
    ) {
        //TODO: Тут необходимо сделать проверку userUid у wallet с пользовательским, чтобы контролировать доступность данных
        //TODO: Либо всегда прокидывать во всех запросах UserUid - это ограничение вывода данных + transaction-service сразу будет знать в какой шард идти

        return transactionClient.getWalletByUid(walletUid.toString());
    }

    @Timed
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WalletResponse> createWallet(
            @Valid @RequestBody Mono<CreateWalletRequest> requestMono,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return extractUserUid(jwt)
                .flatMap(userUid -> requestMono
                        .map(body -> toContractRequest(body, userUid))
                        .flatMap(transactionClient::createWallet)
                );
    }

    private Mono<UUID> extractUserUid(Jwt jwt) {
        Object userUidRaw = jwt.getClaims().get("user_id");

        if (userUidRaw == null) {
            return Mono.error(new IllegalArgumentException("user_id not found in token"));
        }

        try {
            return Mono.just(UUID.fromString(userUidRaw.toString()));
        } catch (IllegalArgumentException ex) {
            return Mono.error(new IllegalArgumentException("Invalid user_id format in token"));
        }
    }

    private com.advanced.contract.model.CreateWalletRequest toContractRequest(CreateWalletRequest body, UUID userUid) {
        return new com.advanced.contract.model.CreateWalletRequest()
                .name(body.name())
                .walletTypeUid(body.walletTypeUid())
                .userUid(userUid);
    }
}
