package io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderPaidEvent(
        Long orderId,
        OrderPaidCustomerPayload customer,
        Instant orderDate,
        BigDecimal orderTotal,
        OrderStatus orderStatus,
        String orderObservations,
        List<OrderPaidItemPayload> orderItems
) {
}
