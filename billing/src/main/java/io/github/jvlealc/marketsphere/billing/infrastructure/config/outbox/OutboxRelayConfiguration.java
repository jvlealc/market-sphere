package io.github.jvlealc.marketsphere.billing.infrastructure.config.outbox;

import io.github.jvlealc.marketsphere.billing.application.document.InvoiceDocumentStoragePort;
import io.github.jvlealc.marketsphere.billing.application.notification.InvoiceNotificationPort;
import io.github.jvlealc.marketsphere.billing.application.outbox.relay.OrderBilledPublisherPort;
import io.github.jvlealc.marketsphere.billing.application.outbox.payload.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.billing.application.outbox.relay.OutboxRelayService;
import io.github.jvlealc.marketsphere.billing.application.outbox.relay.ProcessOrderBilledEmailUseCase;
import io.github.jvlealc.marketsphere.billing.application.outbox.relay.ProcessOrderBilledMessagingUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


/**
 * Monta os dois workers de relay com os parâmetros de operação vindos do YAML.
 * <p>
 * Os casos de uso não são {@code @Component} de propósito: eles precisam de {@code OutboxRelaySettings},
 * e quem sabe de onde esses números vêm é a infraestrutura. Deixá-los se auto-registrar exigiria injetar
 * {@code OutboxRelayProperties} — um tipo de {@code infrastructure} — dentro de {@code application}, que é
 * exatamente a dependência que o {@code ArchitectureTest} proíbe.
 * <p>
 * A construção também é onde a invariante {@code deliveryTimeout < lockDuration} é verificada: um valor
 * incoerente no YAML derruba o boot aqui, com a mensagem que explica a relação.
 */
@Configuration
public class OutboxRelayConfiguration {

    @Bean
    ProcessOrderBilledMessagingUseCase processOrderBilledMessagingUseCase(
            OutboxRelayService outboxRelay,
            OrderBilledPublisherPort orderBilledPublisher,
            OutboxRelayProperties props
    ) {
        return new ProcessOrderBilledMessagingUseCase(
                outboxRelay,
                props.orderBilledMessaging().toSettings(),
                orderBilledPublisher
        );
    }

    @Bean
    ProcessOrderBilledEmailUseCase processOrderBilledEmailUseCase(
            OutboxRelayService outboxRelay,
            OutboxPayloadCodecPort payloadCodec,
            InvoiceDocumentStoragePort invoiceDocumentStorage,
            InvoiceNotificationPort invoiceNotification,
            OutboxRelayProperties props
    ) {
        return new ProcessOrderBilledEmailUseCase(
                outboxRelay,
                payloadCodec,
                invoiceDocumentStorage,
                invoiceNotification,
                props.orderBilledEmail().toSettings()
        );
    }
}
