package io.github.jvlealc.marketsphere.orders.application.output;

import java.math.BigDecimal;

public record OrderItemDetailsOutput(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice,
        boolean active
) {
}
