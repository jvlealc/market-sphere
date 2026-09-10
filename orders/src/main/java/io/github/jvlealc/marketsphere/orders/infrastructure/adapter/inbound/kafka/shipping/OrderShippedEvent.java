package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.shipping;

import java.time.Instant;

public record OrderShippedEvent(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) {
}
