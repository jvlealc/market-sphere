package io.github.jvlealc.marketsphere.billing.infrastructure.exception;

public class OutboxPayloadMappingException extends InfrastructureException {

    public OutboxPayloadMappingException(String message) {
        super(message);
    }

    public OutboxPayloadMappingException(String message, Throwable cause) {
        super(message, cause);
    }
}
