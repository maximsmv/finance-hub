package com.advanced.individualsapi.service;

import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.exception.*;
import com.advanced.individualsapi.integration.KeycloakIntegration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private KeycloakIntegration keycloakIntegration;

    @InjectMocks
    private UserService userService;

    private RegistrationRequest registrationRequest;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;
    private AuthResponse authResponse;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        registrationRequest = new RegistrationRequest("test@example.com", "password", "password");
        loginRequest = new LoginRequest("test@example.com", "password");
        refreshTokenRequest = new RefreshTokenRequest("refresh-token");
        authResponse = new AuthResponse("access-token", 300, "refresh-token", "Bearer");
        userResponse = new UserResponse("user-id", "test@example.com", List.of("USER"), "2025-05-28");
    }

    @Test
    void register_Success_ReturnsAuthResponse() {
        when(keycloakIntegration.register(any(RegistrationRequest.class))).thenReturn(Mono.just(authResponse));

        Mono<AuthResponse> result = userService.register(registrationRequest);

        StepVerifier.create(result)
                .expectNext(authResponse)
                .verifyComplete();

        verify(keycloakIntegration).register(registrationRequest);
    }

    @Test
    void register_PasswordMismatch_ThrowsPasswordMismatchException() {
        RegistrationRequest invalidRequest = new RegistrationRequest("test@example.com", "password", "different");

        Mono<AuthResponse> result = userService.register(invalidRequest);

        StepVerifier.create(result)
                .expectError(PasswordMismatchException.class)
                .verify();

        verify(keycloakIntegration, never()).register(any());
    }

    @Test
    void register_UserAlreadyExists_ThrowsUserAlreadyExistsException() {
        when(keycloakIntegration.register(any(RegistrationRequest.class)))
                .thenReturn(Mono.error(new UserAlreadyExistsException()));

        Mono<AuthResponse> result = userService.register(registrationRequest);

        StepVerifier.create(result)
                .expectError(UserAlreadyExistsException.class)
                .verify();

        verify(keycloakIntegration).register(registrationRequest);
    }

    @Test
    void login_Success_ReturnsAuthResponse() {
        when(keycloakIntegration.login(any(LoginRequest.class))).thenReturn(Mono.just(authResponse));

        Mono<AuthResponse> result = userService.login(loginRequest);

        StepVerifier.create(result)
                .expectNext(authResponse)
                .verifyComplete();

        verify(keycloakIntegration).login(loginRequest);
    }

    @Test
    void login_InvalidCredentials_ThrowsInvalidCredentialsException() {
        when(keycloakIntegration.login(any(LoginRequest.class)))
                .thenReturn(Mono.error(new InvalidCredentialsException()));

        Mono<AuthResponse> result = userService.login(loginRequest);

        StepVerifier.create(result)
                .expectError(InvalidCredentialsException.class)
                .verify();

        verify(keycloakIntegration).login(loginRequest);
    }

    @Test
    void refreshToken_Success_ReturnsAuthResponse() {
        when(keycloakIntegration.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(Mono.just(authResponse));

        Mono<AuthResponse> result = userService.refreshToken(refreshTokenRequest);

        StepVerifier.create(result)
                .expectNext(authResponse)
                .verifyComplete();

        verify(keycloakIntegration).refreshToken(refreshTokenRequest);
    }

    @Test
    void refreshToken_InvalidToken_ThrowsInvalidTokenException() {
        when(keycloakIntegration.refreshToken(any(RefreshTokenRequest.class)))
                .thenReturn(Mono.error(new InvalidRefreshTokenException()));

        Mono<AuthResponse> result = userService.refreshToken(refreshTokenRequest);

        StepVerifier.create(result)
                .expectError(InvalidRefreshTokenException.class)
                .verify();

        verify(keycloakIntegration).refreshToken(refreshTokenRequest);
    }

    @Test
    void getUser_Success_ReturnsUserResponse() {
        String token = "Bearer access-token";
        when(keycloakIntegration.getUserInfo(any(String.class))).thenReturn(Mono.just(userResponse));

        Mono<UserResponse> result = userService.getUser(token);

        StepVerifier.create(result)
                .expectNext(userResponse)
                .verifyComplete();

        verify(keycloakIntegration).getUserInfo(token);
    }

    @Test
    void getUser_InvalidAccessToken_ThrowsInvalidAccessTokenException() {
        String token = "Bearer invalid-access-token";
        when(keycloakIntegration.getUserInfo(any(String.class)))
                .thenReturn(Mono.error(new InvalidAccessTokenException()));

        Mono<UserResponse> result = userService.getUser(token);

        StepVerifier.create(result)
                .expectError(InvalidAccessTokenException.class)
                .verify();

        verify(keycloakIntegration).getUserInfo(token);
    }
}