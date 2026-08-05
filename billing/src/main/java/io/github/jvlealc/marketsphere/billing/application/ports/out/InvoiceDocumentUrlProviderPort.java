package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.application.model.document.InvoiceDocumentUrl;

public interface InvoiceDocumentUrlProviderPort {

    InvoiceDocumentUrl provideFor(String storageKey);
}
