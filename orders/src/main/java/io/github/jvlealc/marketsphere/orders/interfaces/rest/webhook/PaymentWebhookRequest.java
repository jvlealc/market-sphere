package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

/**
 * Contrato para webhook de pagamento fictício
 * Body:
 * {
 *     "orderId": "long",
 *     "paymentKey": "string",
 *     "successful": "boolean"
 *     "observations": "string"
 * }
 * <br><br/>
 * Headers:
 * {
 *     "apiKey": "string"
 * }
 * */
public record PaymentWebhookRequest(
        @NotNull(message = "{order.customerId.required}")
        @Positive(message = "{order.customerId.positive}")
        Long orderId,

        @NotBlank(message = "{payment.key.required}")
        String paymentKey,

        @NotNull(message = "{payment.successful.required}")
        boolean successful,

        String observations,
        Instant paidAt
) {
}
