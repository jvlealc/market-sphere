package io.github.jvlealc.marketsphere.orders.application.model.outbox;

public record SerializedOutboxPayload(String value) {

    public SerializedOutboxPayload {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be null or blank");
        }

        value = value.trim();
    }
}
