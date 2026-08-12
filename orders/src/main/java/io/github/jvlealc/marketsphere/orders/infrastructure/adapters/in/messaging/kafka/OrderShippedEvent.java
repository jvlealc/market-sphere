package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import java.time.Instant;
import java.util.UUID;

public record OrderShippedEvent(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) {
}
