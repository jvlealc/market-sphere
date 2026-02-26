package io.github.jvlealc.marketsphere.shipping.subscriber.event;

import java.time.Instant;

public record OrderBilledEvent(
        Long orderId,
        String invoiceUrl,
        Instant billedAt
) { }
