package io.github.jvlealc.marketsphere.orders.domain.model;

import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderItemException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderDomainException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderRehydrationException;

import java.math.BigDecimal;
import java.util.function.Function;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final Integer amount;
    private final BigDecimal unitPrice;

    // Construtor de criação e reconstituição
    private OrderItem(Long id, Long productId, Integer amount, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.amount = amount;
        this.unitPrice = unitPrice;
    }

    // Factory method para criação
    public static OrderItem createNew(Long productId, Integer amount, BigDecimal unitPrice) {
        validateCreationInvariants(productId, amount, unitPrice);
        return new OrderItem(null, productId, amount, unitPrice);
    }

    // Factory method de reconstituição
    public static OrderItem rehydrate(Long id, Long productId, Integer amount, BigDecimal unitPrice) {
        validateRehydrationInvariants(id, productId, amount, unitPrice);
        return new OrderItem(id, productId, amount, unitPrice);
    }

    public BigDecimal calculateSubtotal() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.amount));
    }

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
    private static void validateCreationInvariants(Long productId, Integer amount, BigDecimal unitPrice) {
        validateOrderItem(productId, amount, unitPrice, InvalidOrderItemException::new);
    }

    private static void validateRehydrationInvariants(Long id, Long productId, Integer amount, BigDecimal unitPrice) {
        if (id == null) {
            throw new OrderRehydrationException("Rehydrated order item must have an ID");
        }

        validateOrderItem(productId, amount, unitPrice, OrderRehydrationException::new);
    }

    private static void validateOrderItem(
            Long productId,
            Integer amount,
            BigDecimal unitPrice,
            Function<String, OrderDomainException> exceptionFactory
    ) {
        if (productId == null) {
            throw exceptionFactory.apply("Product Id cannot be null");
        }

        if (amount == null) {
            throw exceptionFactory.apply("Amount cannot be null");
        }

        if (amount <= 0) {
            throw exceptionFactory.apply("Amount must be greater than zero");
        }

        if (unitPrice == null) {
            throw exceptionFactory.apply("Unit Price cannot be null");
        }

        if (unitPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw exceptionFactory.apply("Unit Price cannot be negative");
        }
    }
}
