package io.github.jvlealc.marketsphere.customers.client;

public class BrasilApiException extends RuntimeException {
    public BrasilApiException(String message) {
        super(message);
    }

    public BrasilApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
