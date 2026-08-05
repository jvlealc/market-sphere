package io.github.jvlealc.marketsphere.billing.application.model.order;

import io.github.jvlealc.marketsphere.billing.application.exception.InvalidOrderPaidSnapshotException;

import java.math.BigDecimal;

public record OrderPaidItem(
        Long productId,
        String productName,
        BigDecimal unitPrice,
        int amount
) {
    public OrderPaidItem {
        if (productId == null ||  productId <= 0) {
            throw new InvalidOrderPaidSnapshotException("Product ID is required and must be positive");
        }

        if (productName == null || productName.isBlank()) {
            throw new InvalidOrderPaidSnapshotException("Product name is required");
        }

        if (unitPrice == null || unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderPaidSnapshotException("Unit price is required and must not be negative");
        }

        if (amount <= 0) {
            throw new InvalidOrderPaidSnapshotException("Amount items must be grater than zero");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(amount));
    }
}
