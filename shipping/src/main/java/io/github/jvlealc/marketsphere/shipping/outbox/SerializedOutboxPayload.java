package io.github.jvlealc.marketsphere.shipping.outbox;

public record SerializedOutboxPayload(String value) {

    public SerializedOutboxPayload {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Outbox payload cannot be null or blank");
        }

        value = value.trim();
    }
}
