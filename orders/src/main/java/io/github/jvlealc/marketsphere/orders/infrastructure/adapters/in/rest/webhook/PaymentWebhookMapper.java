package io.github.jvlealc.marketsphere.orders.interfaces.rest.webhook;

import io.github.jvlealc.marketsphere.orders.application.command.HandlePaymentConfirmationCommand;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PaymentWebhookMapper {

    HandlePaymentConfirmationCommand toPaymentCommand(PaymentWebhookRequest request);
}
