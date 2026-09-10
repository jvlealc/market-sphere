package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.rest.payment.webhook;

import io.github.jvlealc.marketsphere.orders.application.order.payment.HandlePaymentConfirmationCommand;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
interface PaymentWebhookRestMapper {

    @Mapping(source = "webhookEventId", target = "paymentEventId")
    HandlePaymentConfirmationCommand toPaymentCommand(PaymentWebhookRequest request);
}
