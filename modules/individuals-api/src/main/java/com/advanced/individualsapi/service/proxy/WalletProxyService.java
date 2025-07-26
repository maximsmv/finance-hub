package com.advanced.individualsapi.service.proxy;

import com.advanced.contract.api.WalletRestControllerV1Api;
import com.advanced.contract.model.WalletResponse;
import com.advanced.individualsapi.dto.CreateWalletRequest;
import com.advanced.individualsapi.service.util.JwtUtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WalletProxyService {

    private final WalletRestControllerV1Api transactionClient;

    private final JwtUtilService jwtUtil;

    public Flux<WalletResponse> getWalletsByCurrentUser(Jwt jwt) {
        return jwtUtil.extractUserUid(jwt)
                .flatMapMany(userUid -> transactionClient.getWalletsByUser(userUid.toString()));
    }

    public Mono<WalletResponse> getWalletByUid(UUID walletUid) {
        return transactionClient.getWalletByUid(walletUid.toString());
    }

    public Mono<WalletResponse> createWallet(Mono<CreateWalletRequest> requestMono, Jwt jwt) {
        return jwtUtil.extractUserUid(jwt)
                .flatMap(userUid -> requestMono
                        .map(body -> toContractRequest(body, userUid))
                        .flatMap(transactionClient::createWallet)
                );
    }

    private com.advanced.contract.model.CreateWalletRequest toContractRequest(CreateWalletRequest body, UUID userUid) {
        return new com.advanced.contract.model.CreateWalletRequest()
                .name(body.name())
                .walletTypeUid(body.walletTypeUid())
                .userUid(userUid);
    }
}
