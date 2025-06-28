package com.advanced.personservice.controller;


import com.advanced.contract.model.ErrorResponse;
import com.advanced.contract.model.ErrorValidationResponse;
import com.advanced.contract.model.FieldErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorValidationResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        List<FieldErrorResponse> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> new FieldErrorResponse()
                        .field(error.getField())
                        .message(error.getDefaultMessage()))
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        new ErrorValidationResponse()
                                .message("Validation failed")
                                .status(HttpStatus.BAD_REQUEST.value())
                                .errors(errors)
                );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorValidationResponse> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
        List<FieldErrorResponse> errors = ex.getAllErrors().stream()
                .map(error -> {
                    String fieldName = (error instanceof FieldError fieldError)
                            ? fieldError.getField()
                            : "unknow";
                    return new FieldErrorResponse()
                            .field(fieldName)
                            .message(error.getDefaultMessage());
                })
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value())
                .body(
                        new ErrorValidationResponse()
                            .message("Validation failed")
                            .status(HttpStatus.BAD_REQUEST.value())
                            .errors(errors)
                );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<ErrorValidationResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Невалидный JSON: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST.value()).body(
                new ErrorValidationResponse()
                    .message("Malformed JSON or invalid value")
                    .status(HttpStatus.BAD_REQUEST.value())
                    .errors(List.of())
        );
    }

    @ExceptionHandler(EntityNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<ErrorResponse> handleEntityNotFoundException(EntityNotFoundException  ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND.value())
                .body(new ErrorResponse().error(ex.getMessage()).status(HttpStatus.NOT_FOUND.value()));
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        log.error("Непредвиденная ошибка: ", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR.value()).body(
                new ErrorResponse()
                        .error("Unexpected error occurred: " + ex.getMessage())
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        );
    }

}
