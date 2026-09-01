package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderPreparingShipmentPayload;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderShippedPayload;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OutboxPayload;
import io.github.jvlealc.marketsphere.shipping.shipment.Shipment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Única superfície pública da outbox: monta e grava a linha na <strong>mesma transação</strong> da
 * mudança de estado do agregado. Publicar é trabalho do relay, depois do commit.
 * <p>
 * {@code MANDATORY} recusa executar fora de uma transação em vez de abrir uma: {@code save} é
 * transacional por conta própria, então uma chamada sem transação ambiente commitaria a linha
 * sozinha e reintroduziria o dual-write em silêncio.
 */
@Component
public class OutboxMessageWriter {

    private static final int ORDER_PREPARING_SHIPMENT_EVENT_VERSION = 1;
    private static final int ORDER_SHIPPED_EVENT_VERSION = 1;

    private final OutboxPayloadSerializer payloadSerializer;
    private final OutboxJpaRepository repository;
    private final Clock clock;

    OutboxMessageWriter(OutboxPayloadSerializer payloadSerializer, OutboxJpaRepository repository, Clock clock) {
        this.payloadSerializer = payloadSerializer;
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void writeOrderPreparingShipment(Shipment shipment, EventLineage lineage, Instant occurredAt) {
        write(
                shipment,
                OutboxEventType.ORDER_PREPARING_SHIPMENT,
                ORDER_PREPARING_SHIPMENT_EVENT_VERSION,
                occurredAt,
                lineage,
                new OrderPreparingShipmentPayload(shipment.getOrderId())
        );
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void writeOrderShipped(Shipment shipment, EventLineage lineage) {
        write(
                shipment,
                OutboxEventType.ORDER_SHIPPED,
                ORDER_SHIPPED_EVENT_VERSION,
                shipment.getShippedAt(),
                lineage,
                new OrderShippedPayload(shipment.getOrderId(), shipment.getTrackingCode(), shipment.getShippedAt())
        );
    }

    private void write(
            Shipment shipment,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            EventLineage lineage,
            OutboxPayload payload
    ) {
        repository.save(OutboxMessage.createNew(
                shipment.getId().toString(),
                eventType,
                eventVersion,
                occurredAt,
                shipment.getOrderId().toString(),
                lineage,
                payloadSerializer.serialize(payload),
                Instant.now(clock),
                idempotencyKeyOf(shipment.getId(), eventType)
        ));
    }

    private static String idempotencyKeyOf(UUID shipmentId, OutboxEventType eventType) {
        return "%s-shipment-%s".formatted(
                eventType.name().toLowerCase(Locale.ROOT).replace('_', '-'),
                shipmentId
        );
    }
}
