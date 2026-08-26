package io.github.jvlealc.marketsphere.billing.application.usecase;

import io.github.jvlealc.marketsphere.billing.application.exception.InvalidOrderPaidSnapshotException;
import io.github.jvlealc.marketsphere.billing.application.exception.UnbillableOrderException;
import io.github.jvlealc.marketsphere.billing.application.model.document.GeneratedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.model.document.StoredInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.model.order.OrderPaidSnapshot;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentGeneratorPort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentStoragePort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceRepositoryPort;
import io.github.jvlealc.marketsphere.billing.application.service.InvoiceGenerationOutcomeService;
import io.github.jvlealc.marketsphere.billing.domain.model.Invoice;
import io.github.jvlealc.marketsphere.billing.domain.model.InvoiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Transforma um evento {@code ORDER_PAID} em documento de nota fiscal e em uma entrada
 * {@code ORDER_BILLED} na outbox.
 *
 * <h2>Como as falhas são classificadas</h2>
 * {@link UnbillableOrderException} significa que o pedido não é faturável: o dado não é capaz de produzir
 * um documento fiscal, e reprocessar o evento o rejeitaria de novo. É a única falha que marca a nota como
 * {@code FAILED} — estado terminal.
 * <p>
 * O resto propaga, para que o Kafka reentregue. O default é deliberadamente o caso não classificado,
 * porque os dois modos de errar não são simétricos: uma falha transitória não reconhecida e tratada como
 * terminal queima a nota sem caminho de volta — {@link
 * io.github.jvlealc.marketsphere.billing.domain.model.Invoice} recusa gerar depois de falhar —, enquanto
 * uma falha de negócio tratada como transitória custa apenas reentregas até a DLT absorvê-la, com o dado intacto.
 * <p>
 * Retentar é seguro: a {@code storageKey} é derivada de {@code (orderId, invoiceId)}, então um documento
 * regerado sobrescreve o próprio objeto em vez de deixar um órfão no bucket.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class HandleOrderPaidUseCase {

    private final InvoiceRepositoryPort invoiceRepository;
    private final InvoiceDocumentGeneratorPort invoiceDocumentGenerator;
    private final InvoiceDocumentStoragePort invoiceDocumentStorage;
    private final InvoiceGenerationOutcomeService invoiceGenerationOutcome;

    public void execute(OrderPaidSnapshot orderPaid, EventLineage eventLineage) {
        if (orderPaid == null) throw new InvalidOrderPaidSnapshotException("Order must not be null");
        Objects.requireNonNull(eventLineage, "Event lineage must not be null");

        Invoice invoice = findOrCreateInvoice(orderPaid);

        if (invoice.getStatus() != InvoiceStatus.PROCESSING) {
            log.info("Invoice {} for order {} is already {}. Skipping billing processing.",
                    invoice.getId(), invoice.getOrderId(), invoice.getStatus());
            return;
        }

        try {
            GeneratedInvoiceDocument document = invoiceDocumentGenerator.generate(orderPaid);
            StoredInvoiceDocument storedDocument = invoiceDocumentStorage.store(
                    invoice.getId(),
                    invoice.getOrderId(),
                    document
            );

            invoiceGenerationOutcome.confirmGeneration(
                    invoice.getId(),
                    orderPaid.customer(),
                    storedDocument.storageKey(),
                    Instant.now(),
                    eventLineage
            );

        } catch (UnbillableOrderException businessFailure) {
            log.error("Billing rejected order {} as unbillable. Invoice {} will be marked as FAILED.",
                    invoice.getOrderId(), invoice.getId(), businessFailure);

            invoiceGenerationOutcome.recordTerminalFailure(invoice.getId(), businessFailure, Instant.now());
        }
    }

    private Invoice findOrCreateInvoice(OrderPaidSnapshot orderPaid) {
        return invoiceRepository.findByOrderId(orderPaid.orderId())
                .orElseGet(() -> invoiceRepository.save(Invoice.createNew(makeInvoiceId(), orderPaid.orderId())));
    }

    private static UUID makeInvoiceId() {
        return UuidV7.generate();
    }
}
