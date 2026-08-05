package io.github.jvlealc.marketsphere.billing.application.model.notification;

import java.util.UUID;

/**
 * Representa dados da notificação de fatura emitida.
 *
 * <h2>Por que não passar o payload da outbox direto para a porta</h2>
 * {@code OrderBilledEmailPayload} é o <strong>contrato serializado</strong> gravado na tabela — ele existe
 * para ser congelado, versionado e lido de volta. Se ele fosse o parâmetro da porta de notificação, uma v2
 * do contrato de evento mudaria a assinatura de um colaborador que não tem nada a ver com o assunto.
 * <p>
 * Repare também no que <em>não</em> está aqui: a {@code storageKey}. Buscar o documento é decisão do caso de
 * uso; a porta de notificação recebe o documento pronto e não sabe de onde ele veio.
 */
public record InvoiceNotification(
        Long orderId,
        UUID invoiceId,
        String recipientEmail,
        String recipientName
) {

    public InvoiceNotification {
        if (orderId == null || orderId <= 0) {
            throw new IllegalArgumentException("Order ID is required and must be positive");
        }

        if (invoiceId == null) {
            throw new IllegalArgumentException("Invoice ID is required");
        }

        if (recipientEmail == null || recipientEmail.isBlank()) {
            throw new IllegalArgumentException("Recipient email is required");
        }

        if (recipientName == null || recipientName.isBlank()) {
            throw new IllegalArgumentException("Recipient name is required");
        }

        recipientEmail = recipientEmail.trim();
        recipientName = recipientName.trim();
    }
}
