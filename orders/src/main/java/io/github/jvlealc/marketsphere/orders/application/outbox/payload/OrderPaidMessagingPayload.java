package io.github.jvlealc.marketsphere.orders.application.outbox.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.optionalText;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.required;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredAmount;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.outbox.payload.PayloadValidation.requiredItems;

public record OrderPaidMessagingPayload(
        Long orderId,
        OrderPaidCustomerPayload customer,
        Instant orderDate,
        BigDecimal orderTotal,
        String orderObservations,
        List<OrderPaidItemPayload> orderItems
) implements OutboxPayload {

    public OrderPaidMessagingPayload {
        orderId = requiredId(orderId, "orderId");
        customer = required(customer, "customer");
        orderDate = required(orderDate, "orderDate");
        orderTotal = requiredAmount(orderTotal, "orderTotal");
        orderObservations = optionalText(orderObservations);
        orderItems = requiredItems(orderItems, "orderItems");
    }
}
