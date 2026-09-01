package io.github.jvlealc.marketsphere.shipping.outbox;

public class OutboxPayloadSerializationException extends RuntimeException {

    public OutboxPayloadSerializationException(String message) {
        super(message);
    }

    public OutboxPayloadSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
