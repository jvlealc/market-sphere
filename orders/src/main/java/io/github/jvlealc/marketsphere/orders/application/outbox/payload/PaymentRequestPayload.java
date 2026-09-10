package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;

public record PaymentRequestPayload(Long orderId) implements OutboxPayload {

    public PaymentRequestPayload {
        orderId = requiredId(orderId, "Order ID");
    }
}
