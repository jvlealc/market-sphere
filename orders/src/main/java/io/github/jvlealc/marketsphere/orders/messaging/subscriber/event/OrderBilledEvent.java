package io.github.jvlealc.marketsphere.orders.messaging.subscriber.event;

import java.time.Instant;

public record OrderBilledEvent (
        Long orderId,
        String invoiceUrl,
        Instant billedAt
) { }
