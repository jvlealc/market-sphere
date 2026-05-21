package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        String invoiceUrl,
        Instant billedAt
) {
}
