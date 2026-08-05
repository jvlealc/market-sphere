package io.github.jvlealc.marketsphere.billing.application.model.outbox.payload;

import java.time.Instant;
import java.util.UUID;

public record OrderBilledMessagingPayload(
        Long orderId,
        UUID invoiceId,
        Instant billedAt,
        OrderBilledCustomerPayload customer
) implements OrderBilledPayload {

    public OrderBilledMessagingPayload {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID is required and must be positive");
        }

        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID is required");
        }

        if (billedAt == null) {
            throw new IllegalArgumentException("Billing timestamp is required");
        }

        if (customer == null) {
            throw new IllegalArgumentException("Customer is required");
        }
    }
}
