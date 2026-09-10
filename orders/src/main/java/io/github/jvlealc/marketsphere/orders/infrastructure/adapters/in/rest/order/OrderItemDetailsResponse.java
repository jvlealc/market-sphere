package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.rest.order;

import java.math.BigDecimal;

public record OrderItemDetailsResponse(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {
}
