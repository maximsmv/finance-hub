package com.advanced.transactionservice.controller;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.service.WalletService;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/wallets")
public class WalletRestControllerV1 {

    private final WalletService walletService;

    @Timed
    @WithSpan
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return walletService.createWallet(request);
    }

    @Timed
    @WithSpan
    @GetMapping("/{walletUid}/{userUid}")
    public WalletResponse getWalletByUid(@Valid @PathVariable UUID walletUid, @PathVariable UUID userUid) {
        return walletService.getWalletByUid(walletUid, userUid);
    }

    @Timed
    @WithSpan
    @GetMapping("/user/{userUid}")
    public List<WalletResponse> getWalletsByUser(@Valid @PathVariable UUID userUid) {
        return walletService.getWalletsByUser(userUid);
    }


}
