package io.github.jvlealc.marketsphere.orders.application.order.query;

import java.math.BigDecimal;

public record OrderItemDetailsOutput(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {
}
