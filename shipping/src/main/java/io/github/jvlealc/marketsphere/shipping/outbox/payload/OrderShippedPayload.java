package io.github.jvlealc.marketsphere.shipping.outbox.payload;

import java.time.Instant;

public record OrderShippedPayload(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) implements OutboxPayload { }
