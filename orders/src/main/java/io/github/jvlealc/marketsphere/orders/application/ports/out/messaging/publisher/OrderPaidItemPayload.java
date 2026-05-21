package io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher;

import java.math.BigDecimal;

public record OrderPaidItemPayload(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice
) {
}
