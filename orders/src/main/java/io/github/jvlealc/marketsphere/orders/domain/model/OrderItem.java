package io.github.jvlealc.marketsphere.orders.domain.model;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderItemException;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final Integer amount;
    private final BigDecimal unitPrice;

    // Construtor de criação
    private OrderItem(Long productId,  Integer amount, BigDecimal unitPrice) {
        validateOrderItem(productId, amount, unitPrice);
        this.id = null;
        this.productId = productId;
        this.amount = amount;
        this.unitPrice = unitPrice;
    }

    // Construtor de reconstituição
    private OrderItem(Long id, Long productId, Integer amount, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.unitPrice = unitPrice;
    }

    // Factory method para criação
    public static OrderItem createNew(Long productId, Integer amount, BigDecimal unitPrice) {
        return new OrderItem(productId, amount, unitPrice);
    }

    // Factory method de reconstituição
    public static OrderItem rehydrate(Long id, Long productId, Integer amount, BigDecimal unitPrice) {
        if (id == null) {
            throw new InvalidOrderItemException("Rehydrate order item must have an ID");
        }
        validateOrderItem(productId, amount, unitPrice);

        return new OrderItem(id, productId, amount, unitPrice);
    }

    public BigDecimal calculateSubtotal() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.amount));
    }

    // Getters
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Integer getAmount() { return amount; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderItem other = (OrderItem) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Helpers
    private static void validateOrderItem(Long productId, Integer amount, BigDecimal unitPrice) {
        if (productId == null) {
            throw new InvalidOrderItemException("Product Id cannot be null");
        }

        if (amount == null) {
            throw new InvalidOrderItemException("Amount cannot be null");
        }

        if (amount <= 0) {
            throw new InvalidOrderItemException("Amount must be greater than zero");
        }

        if (unitPrice == null) {
            throw new InvalidOrderItemException("Unit Price cannot be null");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new InvalidOrderItemException("Unit Price cannot be negative");
        }
    }
}
