package io.github.jvlealc.marketsphere.shipping.outbox.payload;

public sealed interface OutboxPayload
        permits OrderPreparingShipmentPayload, OrderShippedPayload {
}
