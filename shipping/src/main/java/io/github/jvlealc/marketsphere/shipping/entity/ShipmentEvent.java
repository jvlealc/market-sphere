package io.github.jvlealc.marketsphere.shipping.entity;

import io.github.jvlealc.marketsphere.shipping.entity.enums.ShipmentStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "shipment_events")
public class ShipmentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "shipment_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_shipment_events_shipment_id")
    )
    private Shipment shipment;

    @Enumerated(EnumType.STRING)
    @Column(name = "shipment_status", nullable = false, length = 30)
    private ShipmentStatus shipmentStatus;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    protected ShipmentEvent() {
    }

    public ShipmentEvent(Shipment shipment, String description) {
        if (shipment == null) {
            throw new IllegalStateException("Shipment must not be null");
        }
        if (shipment.getStatus() == ShipmentStatus.CANCELED && (description == null || description.isBlank())) {
            throw new IllegalStateException("Description must not be null or blank when status is CANCELED");
        }

        this.shipment = shipment;
        this.shipmentStatus = shipment.getStatus();
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Shipment getShipment() {
        return shipment;
    }

    public void setShipment(Shipment shipment) {
        this.shipment = shipment;
    }

    public ShipmentStatus getShipmentStatus() {
        return shipmentStatus;
    }

    public void setShipmentStatus(ShipmentStatus shipmentStatus) {
        this.shipmentStatus = shipmentStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ShipmentEvent other = (ShipmentEvent) obj;
        return this.id != null && this.id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ShipmentEvent{" +
                "shipmentId=" + id +
                ", shipmentId=" + (shipment != null ? shipment.getId() : "N/A") +
                ", shipmentStatus=" + shipmentStatus +
                ", description='" + description + '\'' +
                ", occurredAt=" + occurredAt +
                '}';
    }
}

