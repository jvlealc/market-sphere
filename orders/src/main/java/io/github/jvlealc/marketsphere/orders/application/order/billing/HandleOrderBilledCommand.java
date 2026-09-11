package io.github.jvlealc.marketsphere.orders.application.order.billing;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;

import java.time.Instant;

public record HandleOrderBilledCommand(
        Long orderId,
        String invoiceId,
        Instant billedAt
) {
    public HandleOrderBilledCommand {
        if (orderId == null) {
            throw new InvalidCommandException("orderId must not be null");
        }

        if (orderId <= 0L) {
            throw new InvalidCommandException("orderId must be greater than zero");
        }

        if (invoiceId == null || invoiceId.isBlank()) {
            throw new InvalidCommandException("invoiceId must not be blank");
        }

        if (billedAt == null) {
            throw new InvalidCommandException("billedAt must not be null");
        }

        invoiceId = invoiceId.trim();
    }
}
