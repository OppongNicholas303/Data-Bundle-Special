package com.space.space_bundle.exception;

public class ProviderTimeoutException extends RuntimeException {
    public ProviderTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
    public ProviderTimeoutException(String message) {
        super(message);
    }
}
