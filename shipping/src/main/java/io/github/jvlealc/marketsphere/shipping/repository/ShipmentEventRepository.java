package io.github.jvlealc.marketsphere.shipping.repository;

import io.github.jvlealc.marketsphere.shipping.entity.ShipmentEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShipmentEventRepository extends JpaRepository<ShipmentEvent, Long> {
}
