package io.github.jvlealc.marketsphere.orders.application;

public class ExternalServiceException extends ApplicationException {

    public ExternalServiceException(String message) {
        super(message);
    }

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
