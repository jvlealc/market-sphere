package io.github.jvlealc.marketsphere.billing.application.model.document;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

public record InvoiceDocumentUrl(
        URI value,
        Instant expiresAt
) {
    public InvoiceDocumentUrl {
        Objects.requireNonNull(value, "value must not be null");
        Objects.requireNonNull(expiresAt, "expiresAt must not be null");

        if (!value.isAbsolute()) {
            throw new IllegalArgumentException("value must be absolute");
        }
    }

    public boolean isExpired(Instant reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return !reference.isBefore(expiresAt);
    }
}
