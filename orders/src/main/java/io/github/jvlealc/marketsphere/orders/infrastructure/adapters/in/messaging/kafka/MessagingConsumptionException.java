package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

public class MessagingConsumptionException extends RuntimeException {

    public MessagingConsumptionException(String message,  Throwable cause) {
        super(message, cause);
    }
}
