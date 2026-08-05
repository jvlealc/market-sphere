package io.github.jvlealc.marketsphere.billing.application.ports.out;

import io.github.jvlealc.marketsphere.billing.application.model.document.GeneratedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidSnapshot;

public interface InvoiceDocumentGeneratorPort {

    GeneratedInvoiceDocument generate(OrderPaidSnapshot  orderPaid);
}
