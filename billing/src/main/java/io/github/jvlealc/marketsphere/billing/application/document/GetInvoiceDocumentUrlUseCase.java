package io.github.jvlealc.marketsphere.billing.application.document;

import io.github.jvlealc.marketsphere.billing.application.invoice.InvoiceNotFoundException;
import io.github.jvlealc.marketsphere.billing.application.invoice.InvoiceRepositoryPort;
import io.github.jvlealc.marketsphere.billing.domain.invoice.Invoice;
import io.github.jvlealc.marketsphere.billing.domain.invoice.InvoiceStatus;
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
