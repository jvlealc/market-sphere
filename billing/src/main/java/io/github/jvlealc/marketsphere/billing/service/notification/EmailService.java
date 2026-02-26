package io.github.jvlealc.marketsphere.billing.service.notification;

import io.github.jvlealc.marketsphere.billing.model.Order;

/**
 * Sistema de notificação por Email para clientes.
 * */
public interface EmailService {

    /**
     * Envia um email de fatura para o cliente
     * com o PDF da fatura em anexo.
     *
     * @param order O pedido faturado.
     * @param fileName O nome do arquivo PDF no bucket.
     * @param invoiceUrl A URL pré-assinada (para o corpo do e-mail).
     */
    void sendInvoiceEmailWithAttachment(Order order, String fileName, String invoiceUrl);
}
