package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import java.time.Instant;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.*;

public record OrderReadyForShipmentPayload(
        Long orderId,
        Instant billedAt,
        OrderReadyForShipmentCustomerPayload customer
) implements OutboxPayload {

    public OrderReadyForShipmentPayload {
        requiredId(orderId, "orderId");
        required(billedAt, "billedAt");
        required(customer, "customer");
    }

    public record OrderReadyForShipmentCustomerPayload(
            Long customerId,
            String fullName,
            String email
    ) {
        public OrderReadyForShipmentCustomerPayload {
            requiredId(customerId, "customerId");
            requiredText(fullName, "fullName");
            requiredText(email, "email");
        }
    }
}
