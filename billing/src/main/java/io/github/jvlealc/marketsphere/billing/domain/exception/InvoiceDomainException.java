package io.github.jvlealc.marketsphere.billing.domain.exception;

public abstract class InvoiceDomainException extends RuntimeException {

    protected InvoiceDomainException(String message) {
        super(message);
    }

    protected InvoiceDomainException(String message,  Throwable cause) {
        super(message, cause);
    }
}
