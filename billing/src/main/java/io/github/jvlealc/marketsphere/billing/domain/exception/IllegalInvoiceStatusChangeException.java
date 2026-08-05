package io.github.jvlealc.marketsphere.billing.domain.exception;

public class IllegalInvoiceStatusChangeException extends InvoiceDomainException {

    public IllegalInvoiceStatusChangeException(String message) {
        super(message);
    }
}
