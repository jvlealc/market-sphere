package io.github.jvlealc.marketsphere.billing.infrastructure.adapter.outbound.outbox.jackson;

import io.github.jvlealc.marketsphere.billing.infrastructure.InfrastructureException;

public class OutboxPayloadSerializationException extends InfrastructureException {

    public OutboxPayloadSerializationException(String message) {
        super(message);
    }

    public OutboxPayloadSerializationException(String message,  Throwable cause) {
        super(message, cause);
    }
}
