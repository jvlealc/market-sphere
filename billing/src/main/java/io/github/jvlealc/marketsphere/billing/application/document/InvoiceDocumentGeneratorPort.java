package io.github.jvlealc.marketsphere.billing.application.document;

import io.github.jvlealc.marketsphere.billing.application.order.OrderPaidSnapshot;

public interface InvoiceDocumentGeneratorPort {

    GeneratedInvoiceDocument generate(OrderPaidSnapshot  orderPaid);
}
