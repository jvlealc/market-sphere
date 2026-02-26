package io.github.jvlealc.marketsphere.billing.model;

import java.math.BigDecimal;
import java.util.Objects;

public class OrderItem {

     private Long productId;
     private String productName;
     private BigDecimal unitPrice;
     private int amount;

    public OrderItem() { }

    public OrderItem(Long productId, String productName, BigDecimal unitPrice, int amount) {
        this.productId = productId;
        this.productName = productName;
        this.unitPrice = unitPrice;
        this.amount = amount;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public BigDecimal getSubtotal() {
        return BigDecimal.valueOf(this.amount).multiply(this.unitPrice);
    }

    @Override
    public String toString() {
        return "{" +
                "productId=" + productId +
                ", productName='" + productName + '\'' +
                ", unitPrice=" + unitPrice +
                ", amount=" + amount +
                ", subtotal=" + this.getSubtotal() +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem orderItem = (OrderItem) o;
        return amount == orderItem.amount && Objects.equals(productId, orderItem.productId) && Objects.equals(productName, orderItem.productName) && Objects.equals(unitPrice, orderItem.unitPrice);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, productName, unitPrice, amount);
    }
}
