package io.github.jvlealc.marketsphere.shipping.outbox.payload;

public record OrderPreparingShipmentPayload(Long orderId) implements OutboxPayload {
}
