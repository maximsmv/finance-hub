package com.advanced.individualsapi.exception;

public abstract class AuthException extends RuntimeException {
    private final int status;

    public AuthException(String message, int status) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
