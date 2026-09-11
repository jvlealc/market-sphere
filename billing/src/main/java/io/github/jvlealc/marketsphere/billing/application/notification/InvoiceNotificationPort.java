package io.github.jvlealc.marketsphere.billing.application.notification;

import io.github.jvlealc.marketsphere.billing.application.outbox.OutboxDeliveryException;
import io.github.jvlealc.marketsphere.billing.application.document.RetrievedInvoiceDocument;

/**
 * Avisa o cliente de que a nota fiscal dele foi emitida, com o documento anexo.
 * <p>
 * O nome descreve a <strong>intenção</strong>, não o mecanismo: e-mail é a implementação de hoje, e o
 * adaptador — {@code EmailInvoiceNotificationAdapter} — é quem tem o direito de dizer isso no nome. Trocar
 * por SMS ou webhook não deveria tocar nesta interface.
 * <p>
 * O documento chega pronto, como parâmetro. Buscá-lo no armazenamento é orquestração, e orquestração é
 * trabalho do caso de uso: um adaptador de saída que chama outra porta de saída vira um mini-orquestrador
 * escondido na infraestrutura, onde ninguém procura por ele.
 */
public interface InvoiceNotificationPort {

    /**
     * @throws OutboxDeliveryException se a entrega falhar de forma retentável
     */
    void notifyInvoiceIssued(InvoiceNotification notification, RetrievedInvoiceDocument document);
}
