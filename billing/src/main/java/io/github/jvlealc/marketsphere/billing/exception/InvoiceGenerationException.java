package io.github.jvlealc.marketsphere.billing.exception;

public class InvoiceGenerationException extends RuntimeException {

    public InvoiceGenerationException(String message) {
        super(message);
    }

    public InvoiceGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
