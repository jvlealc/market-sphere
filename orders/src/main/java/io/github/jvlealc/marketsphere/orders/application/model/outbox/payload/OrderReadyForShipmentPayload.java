package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

import java.time.Instant;

import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.*;

public record OrderReadyForShipmentPayload(
        Long orderId,
        Instant billedAt,
        OrderReadyForShipmentCustomerPayload customer
) implements OutboxPayload {

    public OrderReadyForShipmentPayload {
        requiredId(orderId, "Order ID");
        required(billedAt, "Billed at");
        required(customer, "Customer");
    }

    public record OrderReadyForShipmentCustomerPayload(
            Long customerId,
            String fullName,
            String email
    ) {
        public OrderReadyForShipmentCustomerPayload {
            requiredId(customerId, "Customer ID");
            requiredText(fullName, "Customer full name");
            requiredText(email, "Customer e-mail");
        }
    }
}
