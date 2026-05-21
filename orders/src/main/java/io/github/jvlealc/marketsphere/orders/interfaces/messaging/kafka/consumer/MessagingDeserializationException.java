package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

/**
 * Exceção lançada quando ocorre um erro ao desserializar uma mensagem
 * recebida do broker de mensageria.
 */
public class MessagingDeserializationException extends RuntimeException {

    public MessagingDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
