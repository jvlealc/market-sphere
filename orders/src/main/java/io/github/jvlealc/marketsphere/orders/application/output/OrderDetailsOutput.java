package io.github.jvlealc.marketsphere.orders.application.output;

import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        String invoiceUrl,
        UUID trackingCode,
        List<OrderItemDetailsOutput> orderItems
) {
}
