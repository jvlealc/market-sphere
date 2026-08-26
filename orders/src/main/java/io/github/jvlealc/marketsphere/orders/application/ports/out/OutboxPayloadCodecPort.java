package io.github.jvlealc.marketsphere.orders.application.ports.out;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.OutboxPayload;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.SerializedOutboxPayload;

/**
 * Converte o payload de um evento entre o objeto tipado e o JSON que vai para a coluna {@code payload}
 * de {@code outbox_message}.
 */
public interface OutboxPayloadCodecPort {

    SerializedOutboxPayload serialize(OutboxPayload payload);

    /**
     * Lança {@link io.github.jvlealc.marketsphere.orders.application.exception.OutboxPayloadDeserializationException}
     * quando o JSON gravado não é mais legível para o tipo pedido. Quem chama no caminho de entrega deve
     * traduzir isso para falha terminal: reler o mesmo JSON não muda o resultado.
     */
    <T extends OutboxPayload> T deserialize(SerializedOutboxPayload payload, Class<T> type);
}
