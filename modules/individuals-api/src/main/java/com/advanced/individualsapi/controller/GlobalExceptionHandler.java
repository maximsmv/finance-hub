package com.advanced.individualsapi.controller;

import com.advanced.individualsapi.dto.ErrorResponse;
import com.advanced.individualsapi.dto.ErrorValidationResponse;
import com.advanced.individualsapi.dto.FieldErrorResponse;
import com.advanced.individualsapi.exception.AuthException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.support.WebExchangeBindException;
import reactor.core.publisher.Mono;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ErrorValidationResponse>> handleAuthException(WebExchangeBindException  ex) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
                .toList();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(new ErrorValidationResponse("Validation failed", HttpStatus.BAD_REQUEST.value(), fieldErrors)));
    }

    @ExceptionHandler(AuthException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthException(AuthException ex) {
        return Mono.just(ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse(ex.getMessage(), ex.getStatus())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

}