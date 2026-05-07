package io.github.jvlealc.marketsphere.orders.messaging.publisher.event;

import java.math.BigDecimal;

public record OrderItemPayload(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {
    private BigDecimal getSubTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(amount));
    }
}
