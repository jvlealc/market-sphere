package io.github.jvlealc.marketsphere.orders.client.banking;

import java.time.Instant;

/**
 * Representa a resposta do gateway bancário/de pagamento.
 */
public record BankingPaymentRepresentation(
    String paymentKey,    // Chave de pagamento simulada
    Integer statusCode, // Status HTTP simulado
    String message,     // Mensagem de mock
    Instant dateTime    // Timestamp da simulação
) { }
