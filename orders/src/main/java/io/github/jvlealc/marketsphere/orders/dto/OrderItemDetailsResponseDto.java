package io.github.jvlealc.marketsphere.orders.dto;

import java.math.BigDecimal;

public record OrderItemDetailsResponseDto(
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
