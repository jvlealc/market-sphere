package io.github.jvlealc.marketsphere.orders.infrastructure.client.mockpayment;

import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Representa a resposta do gateway de pagamento.
 */
public record MockPaymentRepresentation(
        String paymentKey,    // Chave de pagamento simulada
        Integer statusCode, // Status HTTP simulado
        String message,     // Mensagem de mock
        Instant requestedAt    // Timestamp da simulação
) {
    static MockPaymentRepresentation mock(Long orderId, String idempotencyKey) {
        String paymentKey = UUID.nameUUIDFromBytes(
                ("mock-payment-" + idempotencyKey).getBytes(StandardCharsets.UTF_8)
        ).toString();

        return new MockPaymentRepresentation(
                paymentKey,
                HttpStatus.OK.value(),
                "Payment request simulated successfully for order ID: " + orderId,
                Instant.now()
        );
    }
}
