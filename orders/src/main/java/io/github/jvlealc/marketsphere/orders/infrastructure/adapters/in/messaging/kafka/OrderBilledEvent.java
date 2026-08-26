package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        String invoiceId,
        Instant billedAt
) {
}
