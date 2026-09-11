package io.github.jvlealc.marketsphere.billing.application.document;

public interface InvoiceDocumentUrlProviderPort {

    InvoiceDocumentUrl provideFor(String storageKey);
}
