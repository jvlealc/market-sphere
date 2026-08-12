package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

public class MessagingConsumptionException extends RuntimeException {

    public MessagingConsumptionException(String message,  Throwable cause) {
        super(message, cause);
    }
}
