package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.billing;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        String invoiceId,
        Instant billedAt
) {
}
