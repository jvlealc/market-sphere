package io.github.jvlealc.marketsphere.shipping.entity;

import io.github.jvlealc.marketsphere.shipping.entity.enums.ShipmentStatus;
import io.github.jvlealc.marketsphere.shipping.exception.IllegalShipmentStatusChangeException;
import io.github.jvlealc.marketsphere.shipping.exception.InvalidShipmentException;
import jakarta.persistence.*;

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

    @Column(name = "customer_email", nullable = false, length = 150)
    private String customerEmail;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(name = "shipment_email_sent_at")
    private Instant shipmentEmailSentAt;

    @Column(name = "created_at", nullable = false,  updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
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
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Construtor padrão exigido pelo JPA para reconstituição.
     * Não utilize este construtor para criar novos envios na aplicação.
     * Para novas entidades, use o factory method {@link #createPreparingShipment(Long, Instant, String, String)}.
     */
    protected Shipment() {
    }

    public static Shipment createPreparingShipment(Long orderId, Instant billedAt, String customerEmail, String customerName) {
        validateNewShipment(orderId, billedAt, customerEmail, customerName);
        
        Shipment shipment = new Shipment();
        
        shipment.orderId = orderId;
        shipment.status = ShipmentStatus.PREPARING_SHIPMENT;
        shipment.billedAt = billedAt;
        shipment.customerEmail = customerEmail;
        shipment.customerName = customerName;
        
        Instant now = Instant.now();
        shipment.createdAt = now;
        shipment.updatedAt = now;

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
    public String getCustomerEmail() { return customerEmail; }
    public String getCustomerName() { return customerName; }
    public Instant getShipmentEmailSentAt() { return shipmentEmailSentAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public boolean markAsShipped(String trackingCode, String carrier, Instant shippedAt) {
        requireNonBlank(trackingCode, "Tracking code");
        requireNonBlank(carrier, "Carrier");
        requireNonNull(shippedAt, "Shipped at date");

        if (this.status == ShipmentStatus.SHIPPED) {
            boolean hasSameShippingData = this.trackingCode != null && this.trackingCode.equals(trackingCode)
                    && this.carrier != null && this.carrier.equals(carrier);

            if (!hasSameShippingData) {
                throw new IllegalShipmentStatusChangeException("Conflicting shipped data received for an already shipped shipment");
            }

            return false;
        }

        if (isCanceledAlreadyRegistered()) {
            throw new IllegalShipmentStatusChangeException("Canceled shipment cannot be marked as shipped");
        }

        if (!canMarkAsShipped()) {
            return false;
        }

        this.status = ShipmentStatus.SHIPPED;
        this.trackingCode = trackingCode;
        this.carrier = carrier;
        this.shippedAt = shippedAt;

        return true;
    }

    public boolean markAsCanceled(Instant canceledAt) {
        requireNonNull(canceledAt, "Canceled at date");

        if (isAlreadyShipped()) {
            throw new IllegalShipmentStatusChangeException("Shipped shipment cannot be canceled");
        }

        if (isCanceledAlreadyRegistered()) {
            if (this.canceledAt != null && !this.canceledAt.equals(canceledAt)) {
                throw new IllegalShipmentStatusChangeException("Conflicting canceled data received for an already canceled shipment");
            }
            return false;
        }

        this.status = ShipmentStatus.CANCELED;
        this.canceledAt = canceledAt;

        return true;
    }

    public boolean markShipmentEmailAsSent(Instant sentAt) {
        requireNonNull(sentAt, "Shipment email sent at date");

        if (this.shipmentEmailSentAt != null) {
            return false;
        }

        this.shipmentEmailSentAt = sentAt;
        return true;
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

    private boolean canMarkAsShipped() {
        return this.status == ShipmentStatus.PREPARING_SHIPMENT;
    }

    private boolean isAlreadyShipped() {
        return this.status == ShipmentStatus.SHIPPED;
    }

    private boolean isCanceledAlreadyRegistered() {
        return this.status == ShipmentStatus.CANCELED;
    }

    private static void validateNewShipment(Long orderId, Instant billedAt, String customerEmail, String customerName) {
        if (orderId == null) {
            throw new InvalidShipmentException("Order ID is required");
        }

        requireNonNull(billedAt, "Billed at date");
        requireNonBlank(customerEmail, "Customer email");
        requireNonBlank(customerName, "Customer name");
    }

    private static void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidShipmentException(fieldName + " is required");
        }
    }

    private static void requireNonNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new InvalidShipmentException(fieldName + " is required");
        }
    }
}
