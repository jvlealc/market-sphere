package io.github.jvlealc.marketsphere.shipping.exception;

/**
 * Exceção lançada quando ocorre um erro ao serializar uma mensagem
 * para ser enviada a um sistema de mensageria como o Kafka.
 */
public class MessagingSerializationException extends RuntimeException {
    public MessagingSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
