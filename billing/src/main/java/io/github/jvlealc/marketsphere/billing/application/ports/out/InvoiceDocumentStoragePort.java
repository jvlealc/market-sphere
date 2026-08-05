package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.application.model.document.GeneratedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.document.RetrievedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.document.StoredInvoiceDocument;

import java.util.UUID;

public interface InvoiceDocumentStoragePort {

    StoredInvoiceDocument store(UUID invoiceId, Long orderId, GeneratedInvoiceDocument document);

    /**
     * Lê o documento de volta. O tipo de retorno é diferente do de escrita de propósito — ver
     * {@link RetrievedInvoiceDocument}.
     */
    RetrievedInvoiceDocument retrieve(String storageKey);
}
