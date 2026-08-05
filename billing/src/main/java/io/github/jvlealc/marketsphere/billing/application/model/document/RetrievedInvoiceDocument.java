package io.github.jvlealc.marketsphere.billing.application.model.document;

import static java.util.Objects.requireNonNull;

public record RetrievedInvoiceDocument(byte[] content, String contentType) {

    public RetrievedInvoiceDocument {
        requireNonNull(content, "Document content must not be null");

        if (content.length == 0) {
            throw new IllegalArgumentException("Document content must not be empty");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Document content type must not be null or blank");
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
