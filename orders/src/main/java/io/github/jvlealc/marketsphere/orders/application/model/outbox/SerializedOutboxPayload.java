package io.github.jvlealc.marketsphere.orders.application.model.outbox;

public record SerializedOutboxPayload(String value) {

    public SerializedOutboxPayload {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Outbox payload cannot be blank");
        }

        value = value.trim();
    }
}
