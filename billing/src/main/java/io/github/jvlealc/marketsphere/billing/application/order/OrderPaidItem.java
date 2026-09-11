package io.github.jvlealc.marketsphere.billing.application.order;

import java.math.BigDecimal;

public record OrderPaidItem(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int amount
) {
    public OrderPaidItem {
        if (productId == null ||  productId <= 0) {
            throw new InvalidOrderPaidSnapshotException("productId is required and must be positive");
        }

        if (productName == null || productName.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("productName is required");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderPaidSnapshotException("unitPrice is required and must not be negative");
        }

        if (amount <= 0) {
            throw new InvalidOrderPaidSnapshotException("amount must be greater than zero");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(amount));
    }
}
