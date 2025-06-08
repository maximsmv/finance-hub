package com.advanced.individualsapi.exception;

import org.springframework.http.HttpStatus;

public class InvalidAccessTokenException extends AuthException {
    public InvalidAccessTokenException() {
        super("Invalid or expired access token", HttpStatus.UNAUTHORIZED.value());
    }
}
