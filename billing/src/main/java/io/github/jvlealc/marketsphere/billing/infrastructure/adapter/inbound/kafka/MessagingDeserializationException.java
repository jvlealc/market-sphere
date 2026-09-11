package io.github.jvlealc.marketsphere.billing.infrastructure.adapter.inbound.kafka;

import io.github.jvlealc.marketsphere.billing.infrastructure.InfrastructureException;

/**
 * A mensagem recebida do broker não pôde ser desserializada.
 * <p>
 * É não-retentável em {@code KafkaErrorHandlingConfiguration}: bytes que não viram objeto hoje não virão amanhã.
 */
public class MessagingDeserializationException extends InfrastructureException {

    public MessagingDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
