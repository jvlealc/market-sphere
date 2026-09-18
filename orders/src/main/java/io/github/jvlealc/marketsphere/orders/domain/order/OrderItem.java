package io.github.jvlealc.marketsphere.orders.domain.order;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public class OrderItem {

    private final Long id;
    private final Long productId;
    private final String productName;
    private final Integer amount;
    private final BigDecimal unitPrice;

    // Construtor de criação e reconstituição
    private OrderItem(Long id, Long productId, String productName, Integer amount, BigDecimal unitPrice) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.amount = amount;
        this.unitPrice = unitPrice;
    }

    // Factory method para criação
    public static OrderItem createNew(Long productId, String productName, Integer amount, BigDecimal unitPrice) {
        validateCreationInvariants(productId, productName, amount, unitPrice);
        return new OrderItem(null, productId, productName.trim(), amount, unitPrice);
    }

    // Factory method de reconstituição
    public static OrderItem rehydrate(Long id, Long productId, String productName, Integer amount, BigDecimal unitPrice) {
        validateRehydrationInvariants(id, productId, productName, amount, unitPrice);
        return new OrderItem(id, productId, productName, amount, unitPrice);
    }

    public BigDecimal calculateSubtotal() {
        return this.unitPrice.multiply(BigDecimal.valueOf(this.amount));
    }

    public Long getId() { return id; }
    public String getProductName() { return productName; }
    public Long getProductId() { return productId; }
    public Integer getAmount() { return amount; }
    public BigDecimal getUnitPrice() { return unitPrice; }

    public static List<OrderItem> consolidateByProduct(List<OrderItem> items) {
        Objects.requireNonNull(items, "items cannot be null");

        Map<Long, OrderItem> consolidated = new LinkedHashMap<>();

        for (var item : items) {
            consolidated.merge(item.getProductId(), item, OrderItem::sumAmounts);
        }

        return List.copyOf(consolidated.values());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderItem other = (OrderItem) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Helpers
    private static void validateCreationInvariants(Long productId, String productName, Integer amount, BigDecimal unitPrice) {
        validateOrderItem(productId, productName, amount, unitPrice, InvalidOrderItemException::new);
    }

    private static void validateRehydrationInvariants(Long id, Long productId, String productName, Integer amount, BigDecimal unitPrice) {
        if (id == null) {
            throw new OrderRehydrationException("Rehydrated order item must have an ID");
        }

        validateOrderItem(productId, productName, amount, unitPrice, OrderRehydrationException::new);
    }

    private static void validateOrderItem(
            Long productId,
            String productName,
            Integer amount,
            BigDecimal unitPrice,
            Function<String, OrderDomainException> exceptionFactory
    ) {
        if (productId == null) {
            throw exceptionFactory.apply("Product Id cannot be null");
        }

        if (productName == null || productName.isBlank()) {
            throw exceptionFactory.apply("Product name cannot be blank");
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

    private static OrderItem sumAmounts(OrderItem accumulated, OrderItem addition) {
        try {
            return createNew(
                    accumulated.productId,
                    accumulated.productName,
                    Math.addExact(accumulated.amount, addition.amount),
                    accumulated.unitPrice
            );
        } catch (ArithmeticException overflow) {
            throw new InvalidOrderItemException(
                    "Total amount for product '" + accumulated.getProductId() + "' exceeds the maximum supported value"
            );
        }
    }
}
