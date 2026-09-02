package io.github.jvlealc.marketsphere.shipping.shipment;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShipmentJpaRepository extends JpaRepository<Shipment, UUID> {

    boolean existsByOrderId(Long orderId);

    Optional<Shipment> findByOrderId(Long orderId);

    /**
     * A fila do e-mail de confirmacao: nao ha tabela, a consulta e a fila.
     */
    @Query("""
            select s from Shipment s
            where s.status = ShipmentStatus.SHIPPED
              and s.shipmentEmailSentAt is null
              and s.shipmentEmailAttempts < :maxAttempts
              and (s.shipmentEmailNextAttemptAt is null or s.shipmentEmailNextAttemptAt <= :now)
            order by s.shippedAt
            """)
    List<Shipment> findPendingConfirmationEmails(
            @Param("now") Instant now,
            @Param("maxAttempts") int maxAttempts,
            Limit limit
    );
}
