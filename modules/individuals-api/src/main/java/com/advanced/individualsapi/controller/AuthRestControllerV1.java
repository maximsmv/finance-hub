package com.advanced.individualsapi.controller;

import com.advanced.individualsapi.dto.*;
import com.advanced.individualsapi.service.UserService;
import io.micrometer.core.annotation.Timed;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/auth")
public class AuthRestControllerV1 {

    private final UserService userService;

    @Timed
    @WithSpan
    @PostMapping("/registration")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<AuthResponse> register(@Valid @RequestBody RegistrationRequest request) {
        return userService.register(request);
    }

    @Timed
    @WithSpan
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public Mono<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @Timed
    @WithSpan
    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public Mono<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return userService.refreshToken(request);
    }

    @Timed
    @WithSpan
    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public Mono<UserResponse> getUser(@RequestHeader("Authorization") String authorization) {
        return userService.getUser(authorization);
    }
}
