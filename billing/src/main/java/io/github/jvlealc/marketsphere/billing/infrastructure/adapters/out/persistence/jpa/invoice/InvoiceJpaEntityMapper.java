package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.invoice;

import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
class InvoiceJpaEntityMapper {

    public InvoiceJpaEntity toEntity(Invoice invoice) {
        requireNonNull(invoice, "Invoice is null");

        InvoiceJpaEntity entity = new InvoiceJpaEntity();
        entity.setId(invoice.getId());
        entity.setOrderId(invoice.getOrderId());
        entity.setStatus(invoice.getStatus());
        entity.setStorageKey(invoice.getStorageKey());
        entity.setGeneratedAt(invoice.getGeneratedAt());
        entity.setFailedAt(invoice.getFailedAt());
        entity.setFailureReason(invoice.getFailureReason());

        return entity;
    }

    public Invoice toDomain(InvoiceJpaEntity entity) {
        requireNonNull(entity, "Invoice entity is null");
        return Invoice.rehydrate(
                entity.getId(),
                entity.getOrderId(),
                entity.getStatus(),
                entity.getStorageKey(),
                entity.getGeneratedAt(),
                entity.getFailedAt(),
                entity.getFailureReason()
        );
    }

    public void updateEntity(
            Invoice invoice,
            InvoiceJpaEntity entity
    ) {
        requireNonNull(invoice, "Invoice must not be null");
        requireNonNull(entity, "Invoice entity must not be null");

        entity.setStatus(invoice.getStatus());
        entity.setStorageKey(invoice.getStorageKey());
        entity.setGeneratedAt(invoice.getGeneratedAt());
        entity.setFailedAt(invoice.getFailedAt());
        entity.setFailureReason(invoice.getFailureReason());
    }
}
