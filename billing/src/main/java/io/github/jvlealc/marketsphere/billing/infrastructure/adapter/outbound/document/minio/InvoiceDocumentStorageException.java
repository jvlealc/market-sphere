package io.github.jvlealc.marketsphere.billing.infrastructure.adapter.outbound.document.minio;

import io.github.jvlealc.marketsphere.billing.infrastructure.InfrastructureException;

public class InvoiceDocumentStorageException extends InfrastructureException {

    public InvoiceDocumentStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
