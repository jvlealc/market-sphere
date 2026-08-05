package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;

import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepositoryPort {

    Invoice save(Invoice invoice);

    Optional<Invoice> findById(UUID invoiceId);

    Optional<Invoice> findByOrderId(Long orderId);
}
