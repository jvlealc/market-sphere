package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import java.math.BigDecimal;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.required;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredAmount;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;

public record OrderPaidNotificationPayload(
        Long orderId,
        BigDecimal orderTotal,
        OrderPaidCustomerNotificationPayload customer
) implements OutboxPayload {

    public OrderPaidNotificationPayload {
        orderId = requiredId(orderId, "Order ID");
        orderTotal = requiredAmount(orderTotal, "Order total");
        customer = required(customer, "Customer");
    }
}
