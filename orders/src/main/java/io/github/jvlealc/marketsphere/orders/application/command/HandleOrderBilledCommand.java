package io.github.jvlealc.marketsphere.orders.application.command;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;

import java.time.Instant;

public record HandleOrderBilledCommand(
        Long orderId,
        String invoiceId,
        Instant billedAt
) {
    public HandleOrderBilledCommand {
        if (orderId == null) {
            throw new InvalidCommandException("Order ID must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("Order ID must be greater than zero");
        }

        if (invoiceId == null || invoiceId.isBlank()) {
            throw new InvalidCommandException("Invoice ID must not be blank");
        }

        if (billedAt == null) {
            throw new InvalidCommandException("Billed At must not be null");
        }

        invoiceId = invoiceId.trim();
    }
}
