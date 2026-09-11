package io.github.jvlealc.marketsphere.billing.domain.invoice;

public class IllegalInvoiceStatusChangeException extends InvoiceDomainException {

    public IllegalInvoiceStatusChangeException(String message) {
        super(message);
    }
}
