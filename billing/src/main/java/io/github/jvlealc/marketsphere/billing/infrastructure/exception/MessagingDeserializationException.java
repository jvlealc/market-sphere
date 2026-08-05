package io.github.jvlealc.marketsphere.billing.infrastructure.exception;

/**
 * A mensagem recebida do broker não pôde ser desserializada.
 * <p>
 * É não-retentável em {@code KafkaErrorHandlingConfig}: bytes que não viram objeto hoje não virão amanhã.
 */
public class MessagingDeserializationException extends InfrastructureException {

    public MessagingDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
