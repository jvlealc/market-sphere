package io.github.jvlealc.marketsphere.orders.application.exception;

public class ExternalServiceException extends ApplicationException {

    private final String field;

    protected ExternalServiceException(final String message, final String field) {
        super(message);
        this.field = field;
    }

    public ExternalServiceException(final String message, final Throwable cause) {
        super(message, cause);
        this.field = null;
    }
}
