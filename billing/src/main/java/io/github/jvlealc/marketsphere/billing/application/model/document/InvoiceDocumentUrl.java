package io.github.jvlealc.marketsphere.billing.application.model.document;

import java.net.URI;
import java.time.Instant;

import static java.util.Objects.requireNonNull;

public record InvoiceDocumentUrl(
        URI value,
        Instant expiresAt
) {
    public InvoiceDocumentUrl {
        requireNonNull(value, "Document URL must not be null");
        requireNonNull(expiresAt, "Document URL expiration must not be null");

        if (!value.isAbsolute()) {
            throw new IllegalArgumentException("Document URL must be absolute");
        }
    }

    public boolean isExpired(Instant reference) {
        requireNonNull(reference, "Reference instant must not be null");
        return !reference.isBefore(expiresAt);
    }
}
