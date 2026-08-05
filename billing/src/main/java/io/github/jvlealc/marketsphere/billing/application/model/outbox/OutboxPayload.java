package io.github.jvlealc.marketsphere.billing.application.model.outbox;

public record OutboxPayload(String value) {

    public OutboxPayload {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Outbox payload cannot be blank");
        }

        value = value.trim();
    }
}
