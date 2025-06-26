package com.advanced.individualsapi.service;

import com.advanced.contract.api.UserRestControllerV1Api;
import com.advanced.contract.model.UserDto;
import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.PasswordMismatchException;
import com.advanced.individualsapi.integration.KeycloakIntegration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final KeycloakIntegration keycloakIntegration;

    private final UserRestControllerV1Api userRestApi;

    public Mono<AuthResponse> register(RegistrationRequest request) {
        validate(request);

        return userRestApi.createUser(mapUserDto(request))
                .flatMap(createdUser ->
                        keycloakIntegration.register(request)
                                .onErrorResume(keycloakError ->
                                        userRestApi.compensateCreateUser(Objects.requireNonNull(createdUser.getId()))
                                                .onErrorResume(compensateError -> {
                                                    log.error("Ошибка при компенсации создания пользователя: {}", compensateError.getMessage());
                                                    return Mono.empty();
                                                })
                                                .then(Mono.error(keycloakError))
                                )
                )
                .onErrorResume(createUserError -> {
                    log.error("Не удалось создать пользователя в person-service: {}", createUserError.getMessage());
                    return Mono.error(createUserError);
                });
    }

    private UserDto mapUserDto(RegistrationRequest request) {
        UserDto user = new UserDto();
        user.setEmail(request.email());
        return user;
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

    private void validate(RegistrationRequest request) {
        if (!request.password().equals(request.confirmPassword())) {
            throw new PasswordMismatchException();
        }
    }
}
