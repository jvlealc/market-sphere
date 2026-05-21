package io.github.jvlealc.marketsphere.orders.domain.model;


import io.github.jvlealc.marketsphere.orders.domain.exception.IllegalOrderStatusChangeException;
import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderException;
import io.github.jvlealc.marketsphere.orders.domain.exception.InvalidOrderStateException;
import io.github.jvlealc.marketsphere.orders.domain.exception.OrderRehydrationException;
import io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CancellationInfo;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static io.github.jvlealc.marketsphere.orders.domain.model.enums.OrderStatus.*;

public class Order {
    private Long id;
    private final Long customerId;
    private final Instant orderDate;
    private Instant paidAt;
    private Instant billedAt;
    private Instant shippedAt;
    private String paymentKey;
    private String observations;
    private OrderStatus status;
    private final BigDecimal total;
    private UUID trackingCode;
    private String invoiceUrl;
    private final PaymentInfo paymentInfo;
    private final List<OrderItem> orderItems;
    private CancellationInfo cancellationInfo;

    // Construtor de criação
    private Order(Long customerId, PaymentInfo paymentInfo, List<OrderItem> orderItems) {
        validateNewOrder(customerId, paymentInfo, orderItems);

        this.customerId = customerId;
        this.orderDate = Instant.now();
        this.observations = "Placed order. Awaiting payment.";
        this.status = PAYMENT_PENDING;
        this.orderItems = List.copyOf(orderItems);
        this.total = calculateTotal();
        this.paymentInfo = paymentInfo;
    }

    // Construtor de reconstituição
    private Order(
            Long id,
            Long customerId,
            Instant orderDate,
            Instant paidAt,
            Instant billedAt,
            Instant shippedAt,
            String paymentKey,
            String observations,
            OrderStatus status,
            BigDecimal total,
            UUID trackingCode,
            String invoiceUrl,
            PaymentInfo paymentInfo,
            List<OrderItem> orderItems,
            CancellationInfo cancellationInfo
    ) {
        validateRehydratedOrder(id, customerId, orderDate, status, total, paymentInfo, orderItems);
        validateRehydratedStateConsistency(status, paidAt, billedAt, shippedAt, paymentKey, trackingCode, invoiceUrl, cancellationInfo);

        this.id = id;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.paidAt = paidAt;
        this.billedAt = billedAt;
        this.shippedAt = shippedAt;
        this.paymentKey = paymentKey;
        this.observations = observations;
        this.status = status;
        this.total = total;
        this.trackingCode = trackingCode;
        this.invoiceUrl = invoiceUrl;
        this.paymentInfo = paymentInfo;
        this.orderItems = List.copyOf(orderItems);
        this.cancellationInfo = cancellationInfo;
    }

    // Factory method para criação
    public static Order createNew(Long customerId, PaymentInfo paymentInfo, List<OrderItem> orderItems) {
        return new Order(customerId, paymentInfo, orderItems);
    }

    // Factory method de reconstituição
    public static Order rehydrate(
            Long id,
            Long customerId,
            Instant orderDate,
            Instant paidAt,
            Instant billedAt,
            Instant shippedAt,
            String paymentKey,
            String observations,
            OrderStatus status,
            BigDecimal total,
            UUID trackingCode,
            String invoiceUrl,
            PaymentInfo paymentInfo,
            List<OrderItem> orderItems,
            CancellationInfo cancellationInfo
    ) {
        return new Order(
                id, customerId, orderDate, paidAt, billedAt, shippedAt, paymentKey, observations, status, total, trackingCode,
                invoiceUrl, paymentInfo, orderItems, cancellationInfo
        );
    }

    // Getters
    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public Instant getOrderDate() { return orderDate; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getBilledAt() { return billedAt; }
    public Instant getShippedAt() { return shippedAt; }
    public String getPaymentKey() { return paymentKey; }
    public String getObservations() { return observations; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public UUID getTrackingCode() { return trackingCode; }
    public String getInvoiceUrl() { return invoiceUrl; }
    public PaymentInfo getPaymentInfo() { return paymentInfo; }
    public List<OrderItem> getOrderItems() { return orderItems; }
    public CancellationInfo getCancellationInfo() { return cancellationInfo; }

    public boolean isPaymentAlreadyConfirmed() {
        return this.status == PAID
                || this.status == BILLED
                || this.status == PREPARING_SHIPMENT
                || this.status == SHIPPED;
    }

    public boolean isPaymentFailed() {
        return this.status == PAYMENT_ERROR;
    }

    public void registerPaymentRequest(String paymentKey) {
        throwExceptionIfCanceled();

        if (!canInitiatePayment()) {
            throw new IllegalOrderStatusChangeException("Only PAYMENT_PENDING or PAYMENT_ERROR orders can initiate payment");
        }

        validateString(paymentKey, "Payment key");

        this.paymentKey = paymentKey;
        this.observations = "Payment initiated. Waiting for the payment process to complete";
    }

    public boolean markAsPaid(Instant paidAt) {
        throwExceptionIfCanceled();

        if (isPaymentAlreadyConfirmed()) {
            return false;
        }

        if (this.status != PAYMENT_PENDING) {
            throw new IllegalOrderStatusChangeException(PAYMENT_PENDING, PAID);
        }

        validateInstant(paidAt, "Paid at");

        this.status = PAID;
        this.paidAt = paidAt;
        this.observations = "Payment successfully confirmed";

        return true;
    }

    public boolean markPaymentAsFailed(String observations) {
        throwExceptionIfCanceled();

        if (isPaymentAlreadyConfirmed()) {
            return false;
        }

        if (isPaymentFailed()) {
            return false;
        }

        if (this.status != PAYMENT_PENDING) {
            throw new IllegalOrderStatusChangeException(PAYMENT_PENDING, PAYMENT_ERROR);
        }

        this.status = PAYMENT_ERROR;
        this.observations = (observations == null || observations.isBlank())
                ? "Payment failed"
                : observations;

        return true;
    }

    public boolean markAsBilled(String invoiceUrl, Instant billedAt) {
        throwExceptionIfCanceled();

        validateString(invoiceUrl, "Invoice URL");
        validateInstant(billedAt, "Billed at");

        if (isBillingAlreadyRegistered()) {
            if (!hasSameBillingData(invoiceUrl, billedAt)) {
                throw new InvalidOrderStateException("Conflicting billing data received for an already billed order");
            }
            return false;
        }

        if (this.status != PAID) {
            throw new IllegalOrderStatusChangeException(PAID, BILLED);
        }

        this.status = BILLED;
        this.invoiceUrl = invoiceUrl;
        this.billedAt = billedAt;
        this.observations = "Order successfully billed";

        return true;
    }

    public boolean markAsPreparingShipment() {
        throwExceptionIfCanceled();

        if (this.status == PREPARING_SHIPMENT || this.status == SHIPPED) {
            return false;
        }

        if (this.status != BILLED) {
            throw new IllegalOrderStatusChangeException(BILLED, PREPARING_SHIPMENT);
        }

        this.status = PREPARING_SHIPMENT;
        this.observations = "The order is being prepared for shipment";

        return true;
    }

    public boolean markAsShipped(UUID trackingCode, Instant shippedAt) {
        throwExceptionIfCanceled();

        if (trackingCode == null) {
            throw new InvalidOrderException("Tracking code is required");
        }

        validateInstant(shippedAt, "Shipped at");

        if (isShippingAlreadyRegistered()) {
            if (!hasSameShippingData(trackingCode, shippedAt)) {
                throw new InvalidOrderStateException("Conflicting shipping data received for an already shipped order");
            }
            return false;
        }

        if (this.status != PREPARING_SHIPMENT) {
            throw new IllegalOrderStatusChangeException(PREPARING_SHIPMENT, SHIPPED);
        }

        this.status = SHIPPED;
        this.trackingCode = trackingCode;
        this.shippedAt = shippedAt;
        this.observations = "Order successfully shipped";

        return true;
    }

    public void cancel(CancellationInfo cancellationInfo) {
        throwExceptionIfCanceled();
        if (this.status == OrderStatus.SHIPPED) {
            throw new IllegalOrderStatusChangeException("The order cannot be canceled if it has been SHIPPED");
        }

        if (cancellationInfo == null) {
            throw new InvalidOrderException("Cancellation info is required");
        }

        this.status = CANCELED;
        this.cancellationInfo = cancellationInfo;
        this.observations = "Order canceled";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Order other = (Order) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    // Helpers
    private static void validateNewOrder(Long customerId, PaymentInfo paymentInfo, List<OrderItem> orderItems) {
        if (customerId == null) {
            throw new InvalidOrderException("An order must belong to a customer");
        }

        if (paymentInfo == null) {
            throw new InvalidOrderException("An order must contain payment information");
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new InvalidOrderException("An order must contain at least one item");
        }
    }

    private static void validateRehydratedOrder(Long id, Long customerId, Instant orderDate, OrderStatus status, BigDecimal total,
                                                PaymentInfo paymentInfo, List<OrderItem> orderItems) {
        if (id == null) {
            throw new OrderRehydrationException("Rehydrated order must have an ID");
        }

        if (customerId == null) {
            throw new OrderRehydrationException("Rehydrated order must belong to a customer");
        }

        if (orderDate == null) {
            throw new OrderRehydrationException("Rehydrated order must have an order date");
        }

        if (status == null) {
            throw new OrderRehydrationException("Rehydrated order must have a status");
        }

        if (total == null) {
            throw new OrderRehydrationException("Rehydrated order must have a total value");
        }

        if (paymentInfo == null) {
            throw new OrderRehydrationException("Rehydrated order must contain payment information");
        }

        if (orderItems == null || orderItems.isEmpty()) {
            throw new OrderRehydrationException("Rehydrated order must contain at least one item");
        }
    }

    private static void validateRehydratedStateConsistency(OrderStatus status, Instant paidAt, Instant billedAt, Instant shippedAt,
                                                           String paymentKey, UUID trackingCode, String invoiceUrl, CancellationInfo cancellationInfo) {
        if ((status == PAID || status == BILLED || status == PREPARING_SHIPMENT ||  status == SHIPPED)
                && (paymentKey == null || paymentKey.isBlank())) {
            throw  new OrderRehydrationException("Rehydrated order with status " + status + " must have a payment key");
        }

        if ((status == PAID || status == BILLED || status == PREPARING_SHIPMENT ||  status == SHIPPED) && (paidAt == null)) {
            throw new OrderRehydrationException("Rehydrated order with status " + status + " must have paid date");
        }

        if ((status == BILLED || status == PREPARING_SHIPMENT || status == SHIPPED)
                && (billedAt == null || invoiceUrl == null || invoiceUrl.isBlank())) {
            throw new OrderRehydrationException("Rehydrated order with status " + status + " must have a billing data");
        }

        if (status == SHIPPED && (shippedAt == null || trackingCode == null)) {
            throw new OrderRehydrationException("Rehydrated shipped order must have shipping data");
        }

        if (status == CANCELED && cancellationInfo == null) {
            throw new OrderRehydrationException("Rehydrated canceled order must have cancellation information");
        }
    }

    private BigDecimal calculateTotal() {
        return this.orderItems.stream()
                .map(OrderItem::calculateSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private boolean canInitiatePayment() {
        return this.status == PAYMENT_PENDING || this.status == PAYMENT_ERROR;
    }

    private void throwExceptionIfCanceled() {
        if (this.status == CANCELED) {
            throw new IllegalOrderStatusChangeException("The order has been cancelled");
        }
    }

    private boolean isBillingAlreadyRegistered() {
        return this.status == BILLED || this.status == PREPARING_SHIPMENT ||  this.status == SHIPPED;
    }

    private boolean hasSameBillingData(String invoiceUrl, Instant billedAt) {
        return (this.invoiceUrl != null && this.billedAt != null)
                && this.invoiceUrl.equals(invoiceUrl)
                && this.billedAt.equals(billedAt);
    }

    private boolean hasSameShippingData(UUID trackingCode, Instant shippedAt) {
        return (this.trackingCode != null && this.shippedAt != null)
                && this.trackingCode.equals(trackingCode)
                && this.shippedAt.equals(shippedAt);
    }

    private boolean isShippingAlreadyRegistered() {
        return this.status == SHIPPED;
    }

    private void validateInstant(Instant instant, String field) {
        if (instant == null) {
            throw new InvalidOrderException(field + " - is required");
        }
    }

    private void validateString(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(field + " - is required");
        }
    }
}
