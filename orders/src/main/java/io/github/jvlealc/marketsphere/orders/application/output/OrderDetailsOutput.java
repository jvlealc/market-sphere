package io.github.jvlealc.marketsphere.orders.application.output;

import io.github.jvlealc.marketsphere.orders.domain.model.vo.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

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
