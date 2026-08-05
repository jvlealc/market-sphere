package io.github.jvlealc.marketsphere.billing.infrastructure.exception;

public class OutboxPayloadSerializationException extends InfrastructureException {

    public OutboxPayloadSerializationException(String message) {
        super(message);
    }

    public OutboxPayloadSerializationException(String message,  Throwable cause) {
        super(message, cause);
    }
}
