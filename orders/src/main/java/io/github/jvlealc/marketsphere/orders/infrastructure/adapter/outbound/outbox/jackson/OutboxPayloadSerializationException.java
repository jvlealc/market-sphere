package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.outbox.jackson;

import io.github.jvlealc.marketsphere.orders.infrastructure.InfrastructureException;

public class OutboxPayloadSerializationException extends InfrastructureException {

    public OutboxPayloadSerializationException(String message) {
        super(message);
    }

    public OutboxPayloadSerializationException(String message,  Throwable cause) {
        super(message, cause);
    }
}
