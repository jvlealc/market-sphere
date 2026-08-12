package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import io.github.jvlealc.marketsphere.orders.domain.model.enums.PaymentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PaymentInfoRequest(

        @NotBlank(message = "{order.paymentInfo.metadata.required}")
        String metadata,

        @NotNull(message = "{order.paymentInfo.paymentType.required}")
        PaymentType paymentType
) { }
