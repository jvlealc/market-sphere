package io.github.jvlealc.marketsphere.billing.application.model.document;

public record StoredInvoiceDocument(
        String storageKey
) {
    public StoredInvoiceDocument {
        if (storageKey == null || storageKey.isBlank()) {
            throw new IllegalArgumentException("Storage key must not be null or blank");
        }
    }
}