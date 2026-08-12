package io.github.jvlealc.marketsphere.orders.application.output;

import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderDetailsOutput(
        Long orderId,
        CustomerProfile customer,
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
