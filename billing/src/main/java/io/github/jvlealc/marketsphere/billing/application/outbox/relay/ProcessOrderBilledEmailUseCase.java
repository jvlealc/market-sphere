package io.github.jvlealc.marketsphere.billing.application.outbox.relay;

import io.github.jvlealc.marketsphere.billing.application.outbox.UndeliverableOutboxMessageException;
import io.github.jvlealc.marketsphere.billing.application.document.RetrievedInvoiceDocument;
import io.github.jvlealc.marketsphere.billing.application.notification.InvoiceNotification;
import io.github.jvlealc.marketsphere.billing.application.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.outbox.payload.OrderBilledEmailPayload;
import io.github.jvlealc.marketsphere.billing.application.document.InvoiceDocumentStoragePort;
import io.github.jvlealc.marketsphere.billing.application.notification.InvoiceNotificationPort;
import io.github.jvlealc.marketsphere.billing.application.outbox.payload.OutboxPayloadCodecPort;
import lombok.RequiredArgsConstructor;

/**
 * Envia por e-mail as notas enfileiradas no canal {@code EMAIL}, com o PDF anexo.
 * <p>
 * Encadeia três portas — ler o payload gravado, buscar o documento, entregar a notificação —, que é
 * orquestração e por isso mora num caso de uso, não dentro de um adaptador.
 * <p>
 * A única falha terminal deste caminho é payload que não desserializa: o JSON foi congelado numa transação
 * passada, e se não produz mais o record esperado, não vai produzir na próxima tentativa. O resto —
 * MinIO fora do ar, SMTP recusando conexão — é retentável por omissão, tratado pelo
 * {@link OutboxRelayService}.
 */
@RequiredArgsConstructor
public class ProcessOrderBilledEmailUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxPayloadCodecPort payloadCodec;
    private final InvoiceDocumentStoragePort invoiceDocumentStorage;
    private final InvoiceNotificationPort invoiceNotification;
    private final OutboxRelaySettings settings;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.EMAIL,
                OutboxEventType.ORDER_BILLED,
                settings,
                this::deliver
        );
    }

    private void deliver(OutboxMessage message) {
        OrderBilledEmailPayload payload = readPayload(message);
        RetrievedInvoiceDocument document = invoiceDocumentStorage.retrieve(payload.storageKey());

        invoiceNotification.notifyInvoiceIssued(toNotification(payload), document);
    }

    /**
     * Qualquer forma de não conseguir transformar os bytes gravados no
     * record esperado, JSON malformado, campo obrigatório ausente, invariante violada, é a
     * mesma falha de contrato e nenhuma delas melhora com o tempo - não retentável.
     */
    private OrderBilledEmailPayload readPayload(OutboxMessage message) {
        try {
            return payloadCodec.deserialize(message.getPayload(), OrderBilledEmailPayload.class);

        } catch (RuntimeException contractFailure) {
            throw new UndeliverableOutboxMessageException(
                    "Stored ORDER_BILLED e-mail payload of outbox message %s could not be read".formatted(message.getId()),
                    contractFailure
            );
        }
    }

    private static InvoiceNotification toNotification(OrderBilledEmailPayload payload) {
        return new InvoiceNotification(
                payload.orderId(),
                payload.invoiceId(),
                payload.customerEmail(),
                payload.customerName()
        );
    }
}
