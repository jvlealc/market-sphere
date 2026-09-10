package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.outbound.outbox.jpa;

import io.github.jvlealc.marketsphere.orders.infrastructure.InfrastructureException;

public class OutboxPayloadMappingException extends InfrastructureException {

    public OutboxPayloadMappingException(String message) {
        super(message);
    }

    public OutboxPayloadMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
