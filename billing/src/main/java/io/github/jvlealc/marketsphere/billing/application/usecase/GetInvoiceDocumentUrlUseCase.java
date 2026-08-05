package io.github.jvlealc.marketsphere.billing.application.usecase;

import io.github.jvlealc.marketsphere.billing.application.exception.InvoiceDocumentUnavailableException;
import io.github.jvlealc.marketsphere.billing.application.exception.InvoiceNotFoundException;
import io.github.jvlealc.marketsphere.billing.application.model.document.InvoiceDocumentUrl;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentUrlProviderPort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceRepositoryPort;
import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class GetInvoiceDocumentUrlUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final InvoiceDocumentUrlProviderPort invoiceDocumentUrlProvider;

    public InvoiceDocumentUrl execute(UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new InvoiceNotFoundException(invoiceId));

        if (invoice.getStatus() != InvoiceStatus.GENERATED) {
            throw new InvoiceDocumentUnavailableException(invoiceId);
        }

        return invoiceDocumentUrlProvider.provideFor(invoice.getStorageKey());
    }
}
