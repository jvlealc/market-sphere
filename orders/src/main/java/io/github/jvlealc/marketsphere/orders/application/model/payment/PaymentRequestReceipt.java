package io.github.jvlealc.marketsphere.orders.application.model.payment;

import java.time.Instant;

/**
 * Representa a resposta do gateway de pagamento.
 */
public record PaymentRequestReceipt(
        String paymentKey,   // Chave de pagamento simulada
        int statusCode,      // Status HTTP simulado
        String message,      // Mensagem de mock
        Instant requestedAt  // Timestamp da simulação
) { }
