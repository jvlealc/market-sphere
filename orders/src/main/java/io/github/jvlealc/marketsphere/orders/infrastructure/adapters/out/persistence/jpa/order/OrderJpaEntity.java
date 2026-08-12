package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = { "paymentInfo", "orderItems", "cancellationInfo" })
public class OrderJpaEntity implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "order_date", nullable = false, updatable = false)
    private Instant orderDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "billed_at")
    private Instant billedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "payment_key", columnDefinition = "TEXT")
    private String paymentKey;

    @Column
    private String observations;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrderStatus status;

    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal total;

    @Column(name = "tracking_code")
    private UUID trackingCode;

    @Column(name = "invoice_url", columnDefinition = "TEXT")
    private String invoiceUrl;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "order")
    private PaymentInfoJpaEntity paymentInfo;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "order")
    private List<OrderItemJpaEntity> orderItems = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "order")
    private CancellationInfoJpaEntity cancellationInfo;

    public void setPaymentInfo(PaymentInfoJpaEntity paymentInfo) {
        if (paymentInfo != null) {
            paymentInfo.setOrder(this);
        }
        this.paymentInfo = paymentInfo;
    }

    public void setOrderItems(List<OrderItemJpaEntity> orderItems) {
        if (this.orderItems == null) {
            this.orderItems = new ArrayList<>();
        }

        this.orderItems.clear();

        if (orderItems == null || orderItems.isEmpty()) {
            return;
        }

        orderItems.forEach(this::addOrderItem);
    }

    public void setCancellationInfo(CancellationInfoJpaEntity cancellationInfo) {
        if (cancellationInfo != null) {
            cancellationInfo.setOrder(this);
        }
        this.cancellationInfo = cancellationInfo;
    }


    public void addOrderItem(OrderItemJpaEntity orderItem) {
        if (orderItem != null) {
            orderItem.setOrder(this);
        }
        this.orderItems.add(orderItem);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        OrderJpaEntity other = (OrderJpaEntity) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
