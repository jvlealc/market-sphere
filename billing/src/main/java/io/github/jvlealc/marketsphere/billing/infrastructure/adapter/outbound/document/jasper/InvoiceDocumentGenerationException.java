package io.github.jvlealc.marketsphere.billing.infrastructure.adapter.outbound.document.jasper;

import io.github.jvlealc.marketsphere.billing.infrastructure.InfrastructureException;

public class InvoiceDocumentGenerationException extends InfrastructureException {

    public InvoiceDocumentGenerationException(String message) {
        super(message);
    }

    public InvoiceDocumentGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
