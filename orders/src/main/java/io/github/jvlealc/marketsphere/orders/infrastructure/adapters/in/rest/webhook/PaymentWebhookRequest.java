package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.rest.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * Representa o payload recebido pelo webhook de confirmação de pagamento
 * do provedor fictício utilizado no ambiente de desenvolvimento.
 */
public record PaymentWebhookRequest(
        @NotNull(message = "{order.id.required}")
        @Positive(message = "{order.id.positive}")
        Long orderId,

        @NotBlank(message = "{payment.key.required}")
        String paymentKey,

        @NotBlank(message = "{payment.eventId.required}")
        String webhookEventId,

        @NotNull(message = "{payment.success.required}")
        Boolean successful,

        String observations,
        Instant paidAt
) {
}
