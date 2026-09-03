package io.github.jvlealc.marketsphere.billing.application.model.document;

import java.util.Objects;

public record RetrievedInvoiceDocument(byte[] content, String contentType) {

    public RetrievedInvoiceDocument {
        Objects.requireNonNull(content, "content must not be null");

        if (content.length == 0) {
            throw new IllegalArgumentException("content must not be empty");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("contentType must not be null or blank");
        }

        content = content.clone();
        contentType = contentType.trim();
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public long size() {
        return content.length;
    }
}
