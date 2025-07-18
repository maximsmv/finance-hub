package com.advanced.individualsapi.controller;

import com.advanced.contract.model.ErrorResponse;
import com.advanced.contract.model.ErrorValidationResponse;
import com.advanced.contract.model.FieldErrorResponse;
import com.advanced.individualsapi.exception.AuthException;
import com.advanced.individualsapi.exception.PersonServiceIntegrationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(PersonServiceIntegrationException.class)
    public ResponseEntity<?> handlePersonServiceError(PersonServiceIntegrationException ex) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            if (ex.getStatus() == HttpStatus.BAD_REQUEST) {
                ErrorValidationResponse response = mapper.readValue(ex.getBody(), ErrorValidationResponse.class);
                return ResponseEntity.status(ex.getStatus()).body(response);
            } else {
                ErrorResponse response = mapper.readValue(ex.getBody(), ErrorResponse.class);
                return ResponseEntity.status(ex.getStatus()).body(response);
            }
        } catch (Exception e) {
            log.warn("Ошибка десериализации тела ошибки от person-service: {}", e.getMessage());
            return ResponseEntity
                    .status(ex.getStatus())
                    .body(new ErrorResponse()
                            .error("Upstream error: " + ex.getBody())
                            .status(ex.getStatus().value()));
        }
    }

    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<ResponseEntity<ErrorValidationResponse>> handleAuthException(WebExchangeBindException  ex) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse().field(error.getField()).message(error.getDefaultMessage()))
                .toList();
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(new ErrorValidationResponse().message("Validation failed").status(HttpStatus.BAD_REQUEST.value()).errors(fieldErrors)));
    }

    @ExceptionHandler(AuthException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleAuthException(AuthException ex) {
        return Mono.just(ResponseEntity.status(ex.getStatus())
                .body(new ErrorResponse().error(ex.getMessage()).status(ex.getStatus())));
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGenericException(Exception ex) {
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse().error("Unexpected error occurred: " + ex.getMessage()).status(HttpStatus.INTERNAL_SERVER_ERROR.value())));
    }

}