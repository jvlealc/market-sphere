package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import java.math.BigDecimal;

public record OrderItemDetailsResponse(
        Long productId,
        String productName,
        Integer amount,
        BigDecimal unitPrice,
        boolean active
) {

    private BigDecimal getSubTotal() {
        return unitPrice.multiply(BigDecimal.valueOf(amount));
    }
}
