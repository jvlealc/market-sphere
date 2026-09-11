package io.github.jvlealc.marketsphere.billing.domain.invoice;

public class InvalidInvoiceStateException extends InvoiceDomainException {

    public InvalidInvoiceStateException(String message) {
        super(message);
    }
}
