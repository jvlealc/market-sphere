package io.github.jvlealc.marketsphere.billing.domain.exception;

public class InvoiceRehydrationException extends InvoiceDomainException {

    public InvoiceRehydrationException(String message) {
        super("Corruption data - " + message);
    }
}
