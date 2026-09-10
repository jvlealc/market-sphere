package io.github.jvlealc.marketsphere.orders.application.order.query;

import io.github.jvlealc.marketsphere.orders.domain.order.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsOutput(
        Long orderId,
        Long customerId,
        CustomerSnapshot customer,
        Instant orderDate,
        Instant paidAt,
        Instant billedAt,
        Instant shippedAt,
        BigDecimal orderTotal,
        OrderStatus orderStatus,
        String orderObservations,
        String invoiceId,
        String trackingCode,
        List<OrderItemDetailsOutput> orderItems
) {
}
