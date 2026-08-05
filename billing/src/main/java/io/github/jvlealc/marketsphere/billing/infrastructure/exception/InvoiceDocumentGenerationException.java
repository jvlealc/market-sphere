package io.github.jvlealc.marketsphere.billing.infrastructure.exception;

public class InvoiceDocumentGenerationException extends InfrastructureException {

    public InvoiceDocumentGenerationException(String message) {
        super(message);
    }

    public InvoiceDocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
