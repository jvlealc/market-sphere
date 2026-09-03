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
            throw new IllegalArgumentException("orderId must not be null and must be greater than zero");
        }

        if (invoiceId == null) {
            throw new IllegalArgumentException("invoiceId must not be null");
        }

        if (billedAt == null) {
            throw new IllegalArgumentException("billedAt must not be null");
        }

        if (customer == null) {
            throw new IllegalArgumentException("customer must not be null");
        }
    }
}
