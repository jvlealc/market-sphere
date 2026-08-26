package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

import java.time.Instant;

public record HandleOrderShippedCommand(
        Long orderId,
        String trackingCode,
        Instant shippedAt
) {
    public HandleOrderShippedCommand {
        if (orderId == null) {
            throw new InvalidCommandException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("Order ID must be greater than zero");
        }

        if (trackingCode == null || trackingCode.isBlank()) {
            throw new InvalidCommandException("Tracking code must not be blank");
        }

        if (shippedAt == null) {
            throw new InvalidCommandException("Shipped at date must not be null");
        }

        trackingCode = trackingCode.trim();
    }
}
