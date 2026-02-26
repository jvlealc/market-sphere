package io.github.jvlealc.marketsphere.orders.model;


import io.github.jvlealc.marketsphere.orders.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(exclude = {"orderItems"})
public class Order implements Serializable {

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

    @Column(columnDefinition = "TEXT")
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

    @Transient
    private PaymentInfo paymentInfo;

    @OneToMany(mappedBy = "order", orphanRemoval = true)
    private List<OrderItem> orderItems;
}
