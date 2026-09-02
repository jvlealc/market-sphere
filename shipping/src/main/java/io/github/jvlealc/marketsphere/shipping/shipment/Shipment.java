package io.github.jvlealc.marketsphere.shipping.shipment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "shipments")
public class Shipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ShipmentStatus status;

    @Column(name = "billed_at", nullable = false)
    private Instant billedAt;

    @Column(name = "shipped_at")
    private Instant shippedAt;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "tracking_code", length = 120)
    private String trackingCode;

    @Column(length = 100)
    private String carrier;

    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @Column(name = "correlation_id", nullable = false, length = 64)
    private String correlationId;

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "shipment_email_sent_at")
    private Instant shipmentEmailSentAt;

    @Column(name = "shipment_email_attempts", nullable = false)
    private int shipmentEmailAttempts = 0;

    @Column(name = "shipment_email_next_attempt_at")
    private Instant shipmentEmailNextAttemptAt;

    @Column(name = "created_at", nullable = false,  updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    protected void prePersist() {
        var now = Instant.now();

        if (this.status == null) {
            this.status = ShipmentStatus.PREPARING_SHIPMENT;
        }

        if (this.createdAt == null) {
            this.createdAt = now;
        }

        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Exigido pelo JPA. Para criar envios novos, use {@link #createPreparingShipment}.
     */
    protected Shipment() {
    }

    public static Shipment createPreparingShipment(
            Long orderId,
            Instant billedAt,
            Long customerId,
            String customerEmail,
            String customerName,
            String correlationId
    ) {
        if (orderId == null) {
            throw new InvalidShipmentException("Order ID is required");
        }
        if (orderId <= 0L) {
            throw new InvalidShipmentException("Order ID must be greater than zero");
        }

        if (customerId == null) {
            throw new InvalidShipmentException("Customer ID is required");
        }
        if (customerId <= 0L) {
            throw new InvalidShipmentException("Customer ID must be greater than zero");
        }

        var shipment = new Shipment();
        
        shipment.orderId = orderId;
        shipment.billedAt = requireNonNull(billedAt, "Billed at date");
        shipment.customerId = customerId;
        shipment.customerEmail = requireNonBlank(customerEmail, "Customer email");
        shipment.customerName = requireNonBlank(customerName, "Customer name");
        shipment.correlationId = requireNonBlank(correlationId, "Correlation ID");

        Instant now = Instant.now();
        shipment.createdAt = now;
        shipment.updatedAt = now;
        shipment.status = ShipmentStatus.PREPARING_SHIPMENT;

        return shipment;
    }

    // Getters
    public UUID getId() { return id; }
    public Long getOrderId() { return orderId; }
    public ShipmentStatus getStatus() { return status; }
    public Instant getBilledAt() { return billedAt; }
    public Instant getShippedAt() { return shippedAt; }
    public Instant getCanceledAt() { return canceledAt; }
    public String getTrackingCode() { return trackingCode; }
    public String getCarrier() { return carrier; }
    public Long getCustomerId() { return customerId; }
    public String getCorrelationId() { return correlationId; }
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerName() { return customerName; }
    public Instant getShipmentEmailSentAt() { return shipmentEmailSentAt; }
    public int getShipmentEmailAttempts() { return shipmentEmailAttempts; }
    public Instant getShipmentEmailNextAttemptAt() { return shipmentEmailNextAttemptAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean markAsShipped(String trackingCode, String carrier, Instant shippedAt) {
        String normalizedCode = requireNonBlank(trackingCode, "Tracking code");
        String normalizedCarrier = requireNonBlank(carrier, "Carrier");

        if (isAlreadyShipped()) {
            boolean hasSameShippingData = normalizedCode.equals(this.trackingCode)
                    && normalizedCarrier.equals(this.carrier);

            if (!hasSameShippingData) {
                throw new IllegalShipmentStatusChangeException("Conflicting shipped data received for an already shipped shipment");
            }

            return false;
        }

        if (isCanceledAlreadyRegistered()) {
            throw new IllegalShipmentStatusChangeException("Canceled shipment cannot be marked as shipped");
        }

        this.shippedAt = requireNonNull(shippedAt, "Shipped at date");
        this.trackingCode = normalizedCode;
        this.carrier = normalizedCarrier;
        this.status = ShipmentStatus.SHIPPED;

        return true;
    }

    public boolean markAsCanceled(Instant canceledAt) {
        if (isAlreadyShipped()) {
            throw new IllegalShipmentStatusChangeException("Shipped shipment cannot be canceled");
        }

        if (isCanceledAlreadyRegistered()) {
            return false;
        }

        this.canceledAt = requireNonNull(canceledAt, "Canceled at date");
        this.status = ShipmentStatus.CANCELED;

        return true;
    }

    public boolean markShipmentEmailAsSent(Instant sentAt) {
        if (!isAlreadyShipped()) {
            throw new IllegalShipmentStatusChangeException("Only a shipped shipment can have its confirmation e-mail recorded");
        }

        if (this.shipmentEmailSentAt != null) {
            return false;
        }

        this.shipmentEmailSentAt = requireNonNull(sentAt, "Shipment email sent at date");
        this.shipmentEmailNextAttemptAt = null;

        return true;
    }

    public void registerEmailDeliveryFailure(Instant nextAttemptAt) {
        this.shipmentEmailNextAttemptAt = requireNonNull(nextAttemptAt, "Next e-mail attempt date");
        this.shipmentEmailAttempts++;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Shipment other = (Shipment) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Shipment{" +
                "shipmentId=" + id +
                ", orderId=" + orderId +
                ", status=" + status +
                ", billedAt=" + billedAt +
                ", shippedAt=" + shippedAt +
                ", canceledAt=" + canceledAt +
                ", trackingCode='" + trackingCode + '\'' +
                ", carrier='" + carrier + '\'' +
                ", customerEmail='" + customerEmail + '\'' +
                ", customerName='" + customerName + '\'' +
                ", shipmentEmailSentAt=" + shipmentEmailSentAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    private boolean isAlreadyShipped() {
        return this.status == ShipmentStatus.SHIPPED;
    }

    private boolean isCanceledAlreadyRegistered() {
        return this.status == ShipmentStatus.CANCELED;
    }

    private static String normalizeStr(String s) {
        return (s == null) ? null : s.trim();
    }

    private static String requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidShipmentException(fieldName + " is required");
        }

        return normalizeStr(value);
    }

    private static <T>T requireNonNull(T obj, String fieldName) {
        if (obj == null) {
            throw new InvalidShipmentException(fieldName + " is required");
        }

        return obj;
    }
}
