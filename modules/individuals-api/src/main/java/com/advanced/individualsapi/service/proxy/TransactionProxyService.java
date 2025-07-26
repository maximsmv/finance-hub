package com.advanced.individualsapi.service.proxy;

import com.advanced.contract.api.TransactionRestControllerV1Api;
import com.advanced.contract.model.*;
import com.advanced.individualsapi.service.util.JwtUtilService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;


@Service
@RequiredArgsConstructor
public class TransactionProxyService {

    private final TransactionRestControllerV1Api transactionClient;

    private final JwtUtilService jwtUtil;

    public Mono<TransactionInitResponse> init(String type, Mono<InitRequest> requestMono) {
        return requestMono.flatMap(req -> transactionClient.init(type, req));
    }

    public Mono<TransactionConfirmResponse> confirm(String type, Mono<ConfirmRequest> requestMono) {
        return requestMono.flatMap(req -> transactionClient.confirm(type, req));
    }

    public Mono<TransactionStatusResponse> getTransactionStatus(String transactionId) {
        return transactionClient.getTransactionStatus(transactionId);
    }

    public Flux<TransactionStatusResponse> searchTransactions(
            Jwt jwt,
            String walletUid,
            String type,
            String status,
            OffsetDateTime dateFrom,
            OffsetDateTime dateTo,
            Integer page,
            Integer size
    ) {
        return jwtUtil.extractUserUid(jwt)
                .flatMapMany(userUid -> transactionClient.searchTransactions(
                        userUid.toString(),
                        walletUid,
                        type,
                        status,
                        dateFrom,
                        dateTo,
                        page,
                        size
                ));
    }

}
