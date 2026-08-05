package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxPayload;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.payload.OutboxPayloadData;

/**
 * Converte o payload de um evento entre o objeto tipado e o JSON que vai para a coluna {@code payload}
 * de {@code outbox_message}.
 */
public interface OutboxPayloadCodecPort {

    OutboxPayload serialize(OutboxPayloadData payload);

    /**
     * Lê de volta o payload gravado. Usado pelos canais que precisam <em>interpretar</em> o corpo, hoje só
     * o de e-mail que monta a mensagem a partir dele. O canal de mensageria nunca chama este méto-do:
     * publica o JSON verbatim, porque o que foi congelado na transação <strong>é</strong> o contrato.
     */
    <T extends OutboxPayloadData> T deserialize(OutboxPayload payload, Class<T> type);
}
