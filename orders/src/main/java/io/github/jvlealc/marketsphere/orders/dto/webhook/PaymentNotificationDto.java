package io.github.jvlealc.marketsphere.orders.dto.webhook;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Contrato para webhook de pagamento
 * Body:
 * {
 *     "orderId": "number",
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
public record PaymentNotificationDto(

        @NotNull(message = "{order.id.required}")
        @Positive(message = "{order.id.positive}")
        Long orderId,

        String paymentKey,
        boolean successful,
        String observations
) { }
