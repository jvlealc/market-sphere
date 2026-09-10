package io.github.jvlealc.marketsphere.orders.infrastructure.config.outbox;

import io.github.jvlealc.marketsphere.orders.application.notification.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.OrderReadyForShipmentPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.payload.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.orders.application.payment.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.OutboxRelayService;
import io.github.jvlealc.marketsphere.orders.application.order.payment.PaymentRequestRegistrationService;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.ProcessOrderPaidMessagingUseCase;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.ProcessOrderPaidNotificationUseCase;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.ProcessOrderReadyForShipmentUseCase;
import io.github.jvlealc.marketsphere.orders.application.outbox.relay.ProcessPaymentRequestUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Monta um caso de uso de relay por par {@code (canal, tipo de evento)}, cada um com as suas
 * configurações.
 *
 * <p>Os casos de uso não são {@code @Component}: os três dependem de um {@code OutboxRelaySettings}, e
 * deixar o container escolher exigiria três beans do mesmo tipo desambiguados por qualificador — com o
 * risco de um relay silenciosamente rodar com os tempos de outro.
 */
@Configuration
public class OutboxRelayConfiguration {

    @Bean
    ProcessPaymentRequestUseCase processPaymentRequestUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProperties props,
            PaymentGatewayPort paymentGateway,
            PaymentRequestRegistrationService paymentRequestRegistrationService
    ) {
        return new ProcessPaymentRequestUseCase(
                outboxRelay,
                props.paymentRequest().toSettings(),
                paymentGateway,
                paymentRequestRegistrationService
        );
    }

    @Bean
    ProcessOrderPaidMessagingUseCase processOrderPaidMessagingUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProperties props,
            OrderPaidPublisherPort orderPaidPublisher
    ) {
        return new ProcessOrderPaidMessagingUseCase(
                outboxRelay,
                props.orderPaidMessaging().toSettings(),
                orderPaidPublisher
        );
    }

    @Bean
    ProcessOrderReadyForShipmentUseCase processOrderReadyForShipmentUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProperties props,
            OrderReadyForShipmentPublisherPort orderReadyForShipmentPublisher
    ) {
        return new ProcessOrderReadyForShipmentUseCase(
                outboxRelay,
                props.orderReadyForShipment().toSettings(),
                orderReadyForShipmentPublisher
        );
    }

    @Bean
    ProcessOrderPaidNotificationUseCase processOrderPaidNotificationUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProperties props,
            OutboxPayloadCodecPort payloadCodec,
            NotificationPort notificationPort
    ) {
        return new ProcessOrderPaidNotificationUseCase(
                outboxRelay,
                props.orderPaidEmail().toSettings(),
                payloadCodec,
                notificationPort
        );
    }
}
