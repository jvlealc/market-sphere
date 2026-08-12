package io.github.jvlealc.marketsphere.orders.application.command;

import java.time.Instant;

public record HandleOrderShippedCommand(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) {
}
