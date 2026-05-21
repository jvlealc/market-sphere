package io.github.jvlealc.marketsphere.orders.application.output;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;

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
