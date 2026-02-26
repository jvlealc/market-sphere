package io.github.jvlealc.marketsphere.shipping.exception;

/**
 * Exceção lançada quando ocorre um erro ao desserializar uma mensagem
 * para ser enviada a um sistema de mensageria como o Kafka.
 */
public class MessagingDeserializationException extends RuntimeException {
    public MessagingDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
