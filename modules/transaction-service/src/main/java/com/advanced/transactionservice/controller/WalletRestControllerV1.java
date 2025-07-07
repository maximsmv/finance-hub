package com.advanced.transactionservice.controller;

import com.advanced.contract.model.CreateWalletRequest;
import com.advanced.contract.model.WalletResponse;
import com.advanced.transactionservice.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/wallets")
public class WalletRestControllerV1 {

    private final WalletService walletService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request) {
        return walletService.createWallet(request);
    }

    @GetMapping("/{walletUid}")
    public WalletResponse getWalletByUid(@PathVariable String walletUid) {
        return walletService.getWalletByUid(walletUid);
    }

    @GetMapping("/user/{userUid}")
    public List<WalletResponse> getWalletsByUser(@PathVariable String userUid) {
        return walletService.getWalletsByUser(userUid);
    }


}
