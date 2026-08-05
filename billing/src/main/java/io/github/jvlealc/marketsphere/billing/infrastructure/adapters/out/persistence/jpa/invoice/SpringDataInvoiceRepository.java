package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.invoice;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface SpringDataInvoiceRepository extends JpaRepository<InvoiceJpaEntity, UUID> {

    Optional<InvoiceJpaEntity> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
