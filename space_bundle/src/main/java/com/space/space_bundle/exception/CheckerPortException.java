package com.space.space_bundle.exception;

public class CheckerPortException extends RuntimeException {
    private final String errorCode;

    public CheckerPortException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
