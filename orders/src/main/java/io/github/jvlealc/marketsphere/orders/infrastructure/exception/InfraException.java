package io.github.jvlealc.marketsphere.orders.infrastructure.exception;

public abstract class InfraException extends RuntimeException {

    public InfraException(String message) {
        super(message);
    }

    public InfraException(String message, Throwable cause) {
        super(message, cause);
    }
}
