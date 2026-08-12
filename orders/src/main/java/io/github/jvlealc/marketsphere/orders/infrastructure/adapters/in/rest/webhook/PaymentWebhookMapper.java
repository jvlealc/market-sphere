package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.rest.webhook;

import io.github.jvlealc.marketsphere.orders.application.command.HandlePaymentConfirmationCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface PaymentWebhookMapper {

    @Mapping(source = "webhookEventId", target = "paymentEventId")
    HandlePaymentConfirmationCommand toPaymentCommand(PaymentWebhookRequest request);
}
