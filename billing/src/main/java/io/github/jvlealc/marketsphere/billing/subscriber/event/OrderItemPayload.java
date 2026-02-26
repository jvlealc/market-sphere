package io.github.jvlealc.marketsphere.billing.subscriber.event;

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
