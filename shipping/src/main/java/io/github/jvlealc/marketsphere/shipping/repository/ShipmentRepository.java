package io.github.jvlealc.marketsphere.shipping.repository;

import io.github.jvlealc.marketsphere.shipping.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {

    boolean existsByOrderId(Long orderId);

    Optional<Shipment> findByOrderId(Long orderId);
}
