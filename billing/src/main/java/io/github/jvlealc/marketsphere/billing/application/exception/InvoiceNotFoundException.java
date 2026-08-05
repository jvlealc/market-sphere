package io.github.jvlealc.marketsphere.billing.application.exception;

import java.util.UUID;

/**
 * A nota não existe mais no repositório.
 * <p>
 * Estende {@link ApplicationException} diretamente, e <strong>não</strong>
 * {@link UnbillableOrderException}: sumir do banco não é motivo para condenar a nota. Propagando, o Kafka
 * reentrega e o use case recria o que faltava. Se fosse classificada como terminal, seria capturada e o
 * registro do desfecho se perderia — com o PDF já no bucket.
 */
public class InvoiceNotFoundException extends ApplicationException {

    public InvoiceNotFoundException(UUID invoiceId) {
        super("Invoice %s was not found".formatted(invoiceId));
    }

    public InvoiceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
