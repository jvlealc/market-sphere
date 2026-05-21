package io.github.jvlealc.marketsphere.orders.application.command;

import java.time.Instant;

public record HandleOrderBilledCommand(
        Long orderId,
        String invoiceUrl,
        Instant billedAt
) {
}
