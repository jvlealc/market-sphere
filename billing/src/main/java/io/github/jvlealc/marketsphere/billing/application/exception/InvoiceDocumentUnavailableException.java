package io.github.jvlealc.marketsphere.billing.application.exception;

import java.util.UUID;

public class InvoiceDocumentUnavailableException extends ApplicationException {

    public InvoiceDocumentUnavailableException(UUID invoiceId) {
        super("Invoice document is not available. Invoice ID: %s".formatted(invoiceId));
    }
}
