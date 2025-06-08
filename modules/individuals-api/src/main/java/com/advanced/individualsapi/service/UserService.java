package com.advanced.individualsapi.service;

import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.PasswordMismatchException;
import com.advanced.individualsapi.integration.KeycloakIntegration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class UserService {

    private final KeycloakIntegration keycloakIntegration;

    public UserService(KeycloakIntegration keycloakIntegration) {
        this.keycloakIntegration = keycloakIntegration;
    }

    public Mono<AuthResponse> register(RegistrationRequest request) {
        validate(request);
        return keycloakIntegration.register(request);
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
