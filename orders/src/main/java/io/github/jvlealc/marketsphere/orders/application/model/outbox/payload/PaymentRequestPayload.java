package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredId;

public record PaymentRequestPayload(Long orderId) implements OutboxPayload {

    public PaymentRequestPayload {
        orderId = requiredId(orderId, "Order ID");
    }
}
