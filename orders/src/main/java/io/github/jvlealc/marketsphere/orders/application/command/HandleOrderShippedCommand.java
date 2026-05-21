package io.github.jvlealc.marketsphere.orders.application.command;

import java.time.Instant;
import java.util.UUID;

public record HandleOrderShippedCommand(
        Long orderId,
        UUID trackingCode,
        Instant shippedAt
) {
}
