package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import io.github.jvlealc.marketsphere.orders.application.ports.out.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.service.OutboxRelayService;
import io.github.jvlealc.marketsphere.orders.application.service.PaymentRequestRegistrationService;
import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessOrderPaidMessagingUseCase;
import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessOrderPaidNotificationUseCase;
import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessPaymentRequestUseCase;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.OutboxRelayProps;
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
public class OutboxRelayConfig {

    @Bean
    ProcessPaymentRequestUseCase processPaymentRequestUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProps props,
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
            OutboxRelayProps props,
            OrderPaidPublisherPort orderPaidPublisher
    ) {
        return new ProcessOrderPaidMessagingUseCase(
                outboxRelay,
                props.orderPaidMessaging().toSettings(),
                orderPaidPublisher
        );
    }

    @Bean
    ProcessOrderPaidNotificationUseCase processOrderPaidNotificationUseCase(
            OutboxRelayService outboxRelay,
            OutboxRelayProps props,
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
