package io.github.jvlealc.marketsphere.orders.application.order.query;

import io.github.jvlealc.marketsphere.orders.domain.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderSummaryOutput(
        Long id,
        Long customerId,
        Instant orderDate,
        String observations,
        OrderStatus status,
        BigDecimal total,
        int amountItems
) {
}
