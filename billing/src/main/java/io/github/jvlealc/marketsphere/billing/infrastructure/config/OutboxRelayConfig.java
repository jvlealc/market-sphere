package io.github.jvlealc.marketsphere.billing.infrastructure.config;

import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceDocumentStoragePort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.InvoiceNotificationPort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OrderBilledPublisherPort;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.billing.application.service.OutboxRelayService;
import io.github.jvlealc.marketsphere.billing.application.usecase.ProcessOrderBilledEmailUseCase;
import io.github.jvlealc.marketsphere.billing.application.usecase.ProcessOrderBilledMessagingUseCase;
import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.OutboxRelayProps;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Monta os dois workers de relay com os parâmetros de operação vindos do YAML.
 * <p>
 * Os casos de uso não são {@code @Component} de propósito: eles precisam de {@code OutboxRelaySettings},
 * e quem sabe de onde esses números vêm é a infraestrutura. Deixá-los se auto-registrar exigiria injetar
 * {@code OutboxRelayProps} — um tipo de {@code infrastructure} — dentro de {@code application}, que é
 * exatamente a dependência que o {@code ArchitectureTest} proíbe.
 * <p>
 * A construção também é onde a invariante {@code deliveryTimeout < lockDuration} é verificada: um valor
 * incoerente no YAML derruba o boot aqui, com a mensagem que explica a relação.
 */
@Configuration
public class OutboxRelayConfig {

    /**
     * O Boot não registra um {@code Clock}, e {@link OutboxRelayService} precisa de um para que a parada por
     * prazo de lease seja verificável sem dormir dentro do teste.
     */
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    ProcessOrderBilledMessagingUseCase processOrderBilledMessagingUseCase(
            OutboxRelayService outboxRelay,
            OrderBilledPublisherPort orderBilledPublisher,
            OutboxRelayProps props
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
            OutboxRelayProps props
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
