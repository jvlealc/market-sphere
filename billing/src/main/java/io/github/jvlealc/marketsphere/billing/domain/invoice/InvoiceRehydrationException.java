package io.github.jvlealc.marketsphere.billing.domain.invoice;

public class InvoiceRehydrationException extends InvoiceDomainException {

    public InvoiceRehydrationException(String message) {
        super("Corruption data - " + message);
    }
}
