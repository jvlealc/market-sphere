package io.github.jvlealc.marketsphere.billing.domain.invoice;

public class InvalidInvoiceException extends InvoiceDomainException {

    public InvalidInvoiceException(String message) {
        super(message);
    }
}
