package io.github.jvlealc.marketsphere.orders.dto;

import io.github.jvlealc.marketsphere.orders.model.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentInfoRequestDto(

        @NotBlank(message = "{order.paymentInfo.metadata.required}")
        String metadata,

        @NotNull(message = "{order.paymentInfo.paymentType.required}")
        PaymentType paymentType
) { }
