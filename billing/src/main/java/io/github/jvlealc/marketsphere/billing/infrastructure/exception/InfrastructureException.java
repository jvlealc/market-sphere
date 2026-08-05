package io.github.jvlealc.marketsphere.billing.infrastructure.exception;

public abstract class InfrastructureException extends RuntimeException {

    public InfrastructureException(String message) {
        super(message);
    }

    public InfrastructureException(String message, Throwable cause) {
        super(message,  cause);
    }
}
