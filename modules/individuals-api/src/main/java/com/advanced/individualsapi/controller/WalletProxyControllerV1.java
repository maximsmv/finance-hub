package com.advanced.individualsapi.controller;

import com.advanced.contract.model.WalletResponse;
import com.advanced.individualsapi.dto.CreateWalletRequest;
import com.advanced.individualsapi.service.proxy.WalletProxyService;
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

    private final WalletProxyService proxyService;

    @Timed
    @WithSpan
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public Flux<WalletResponse> getWalletsByCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return proxyService.getWalletsByCurrentUser(jwt);
    }

    @Timed
    @WithSpan
    @GetMapping("/{walletUid}")
    @ResponseStatus(HttpStatus.OK)
    public Mono<WalletResponse> getWalletByUid(@PathVariable UUID walletUid, @AuthenticationPrincipal Jwt jwt) {
        return proxyService.getWalletByUid(walletUid, jwt);
    }

    @Timed
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<WalletResponse> createWallet(
            @Valid @RequestBody Mono<CreateWalletRequest> requestMono,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return proxyService.createWallet(requestMono, jwt);
    }
}
