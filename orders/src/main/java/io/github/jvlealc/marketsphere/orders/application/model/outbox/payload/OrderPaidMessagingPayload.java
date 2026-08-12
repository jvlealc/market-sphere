package io.github.jvlealc.marketsphere.orders.application.model.outbox.payload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.optionalText;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.required;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredAmount;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredId;
import static io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.PayloadValidation.requiredItems;

public record OrderPaidMessagingPayload(
        Long orderId,
        OrderPaidCustomerPayload customer,
        Instant orderDate,
        BigDecimal orderTotal,
        String orderObservations,
        List<OrderPaidItemPayload> orderItems
) implements OutboxPayload {

    public OrderPaidMessagingPayload {
        orderId = requiredId(orderId, "Order ID");
        customer = required(customer, "Customer");
        orderDate = required(orderDate, "Order date");
        orderTotal = requiredAmount(orderTotal, "Order total");
        orderObservations = optionalText(orderObservations);
        orderItems = requiredItems(orderItems, "Order items");
    }
}
