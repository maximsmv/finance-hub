package com.advanced.individualsapi.exception;

import org.springframework.http.HttpStatus;

public class PasswordMismatchException extends AuthException {
    public PasswordMismatchException() {
        super("Password confirmation does not match", HttpStatus.BAD_REQUEST.value());
    }
}
