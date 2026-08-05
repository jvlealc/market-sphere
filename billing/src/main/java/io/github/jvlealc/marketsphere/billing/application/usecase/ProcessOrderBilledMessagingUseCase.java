package io.github.jvlealc.marketsphere.billing.application.usecase;

import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxRelaySettings;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OrderBilledPublisherPort;
import io.github.jvlealc.marketsphere.billing.application.service.OutboxRelayService;
import lombok.RequiredArgsConstructor;

/**
 * Publica no Kafka as mensagens de {@code ORDER_BILLED} enfileiradas no canal {@code MESSAGING}.
 * <p>
 * O caso de uso é só a amarração: escolhe o par {@code (canal, tipo)}, os parâmetros de operação.
 * O resto, posse do lease, classificação de falha, conclusão, é responsabilidade do {@link OutboxRelayService},
 * um algoritmo só para os dois canais.
 * <p>
 * Não é anotado com {@code @Component}: as configurações vêm de {@code OutboxRelayProps}, que é
 * infraestrutura, então quem monta este bean é {@code OutboxRelayConfig}.
 */
@RequiredArgsConstructor
public class ProcessOrderBilledMessagingUseCase {

    private final OutboxRelayService outboxRelay;
    private final OutboxRelaySettings settings;
    private final OrderBilledPublisherPort orderBilledPublisher;

    public void execute() {
        outboxRelay.relay(
                OutboxChannel.MESSAGING,
                OutboxEventType.ORDER_BILLED,
                settings,
                orderBilledPublisher::publish
        );
    }
}
