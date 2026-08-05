package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.messaging.kafka;

import java.math.BigDecimal;

public record OrderPaidItemPayload(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {

    private BigDecimal getSubTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(amount));
    }
}
