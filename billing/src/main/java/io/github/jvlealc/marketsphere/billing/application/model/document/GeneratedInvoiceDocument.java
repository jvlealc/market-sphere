package io.github.jvlealc.marketsphere.billing.application.model.document;

import static java.util.Objects.requireNonNull;

public record GeneratedInvoiceDocument(
        byte[] content,
        String contentType,
        String fileExtension
) {
    public GeneratedInvoiceDocument {
        requireNonNull(content, "Document content must not be null");

        if (content.length == 0 ) {
            throw new IllegalArgumentException("Document content must not be empty");
        }

        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Document content type must not be null or blank");
        }

        if (fileExtension == null || fileExtension.isBlank()) {
            throw new IllegalArgumentException("Document file extension must not be null or blank");
        }

        content = content.clone();
        contentType = contentType.trim();
        fileExtension = normalizeExtension(fileExtension);
    }

    @Override
    public byte[] content() {
        return content.clone();
    }

    public long size() {
        return content.length;
    }

    private static String normalizeExtension(String fileExtension) {
        String normalized =  fileExtension.trim().toLowerCase();
        return normalized.startsWith(".")
                ? normalized.substring(1)
                : normalized;
    }
}
