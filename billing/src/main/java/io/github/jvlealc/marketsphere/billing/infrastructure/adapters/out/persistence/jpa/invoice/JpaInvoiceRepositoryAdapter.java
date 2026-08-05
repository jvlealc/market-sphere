package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.invoice;


import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceRepositoryPort;
import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JpaInvoiceRepositoryAdapter implements InvoiceRepositoryPort {

    private final SpringDataInvoiceRepository springDataInvoiceRepository;
    private final InvoiceJpaEntityMapper  invoiceJpaEntityMapper;

    @Transactional
    @Override
    public Invoice save(Invoice invoice) {
        InvoiceJpaEntity entity = springDataInvoiceRepository.findById(invoice.getId())
                .map(existingEntity -> {
                    invoiceJpaEntityMapper.updateEntity(invoice, existingEntity);
                    return existingEntity;
                })
                .orElseGet(() -> springDataInvoiceRepository.save(invoiceJpaEntityMapper.toEntity(invoice)));

        return invoiceJpaEntityMapper.toDomain(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findById(UUID invoiceId) {
        return springDataInvoiceRepository.findById(invoiceId)
                .map(invoiceJpaEntityMapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Invoice> findByOrderId(Long orderId) {
        return springDataInvoiceRepository.findByOrderId(orderId)
                .map(invoiceJpaEntityMapper::toDomain);
    }
}
