package com.advanced.individualsapi.service;

import com.advanced.contract.api.UserRestControllerV1Api;
import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.PasswordMismatchException;
import com.advanced.individualsapi.exception.PersonServiceIntegrationException;
import com.advanced.individualsapi.integration.KeycloakIntegration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final KeycloakIntegration keycloakIntegration;

    private final UserRestControllerV1Api userRestApi;

    public Mono<AuthResponse> register(RegistrationRequest request) {
        return validate(request)
                .then(Mono.defer(() -> userRestApi.createUser(request.user())))
                .onErrorResume(WebClientResponseException.class, ex -> Mono.error(new PersonServiceIntegrationException(
                        ex.getStatusCode(), ex.getResponseBodyAsString()
                )))
                .flatMap(createdUser ->
                        keycloakIntegration.register(request)
                                .onErrorResume(keycloakError -> {
                                    log.warn("Выполнение компенсирующего действия по удалению пользователя с id = {}", createdUser.getId());
                                    return userRestApi.compensateCreateUser(Objects.requireNonNull(createdUser.getId()))
                                            .onErrorResume(compensateError -> {
                                                log.error("Ошибка при компенсации создания пользователя: {}", compensateError.getMessage());
                                                return Mono.empty();
                                            })
                                            .then(Mono.error(keycloakError));
                                })
                );
    }

    public Mono<AuthResponse> login(LoginRequest request) {
        return keycloakIntegration.login(request);
    }

    public Mono<AuthResponse> refreshToken(RefreshTokenRequest request) {
        return keycloakIntegration.refreshToken(request);
    }

    public Mono<UserResponse> getUser(String token) {
        return keycloakIntegration.getUserInfo(token);
    }

    private Mono<Void> validate(RegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            return Mono.error(new PasswordMismatchException());
        }
        return Mono.empty();
    }
}
