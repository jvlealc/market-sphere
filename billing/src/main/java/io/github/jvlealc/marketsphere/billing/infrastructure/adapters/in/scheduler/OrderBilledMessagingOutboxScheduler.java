package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.scheduler;

import io.github.jvlealc.marketsphere.billing.application.usecase.ProcessOrderBilledMessagingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gatilho do relay do canal {@code MESSAGING}. Adaptador de entrada como qualquer outro — quem dispara é o
 * relógio, do mesmo modo que o {@code @KafkaListener} é disparado pelo broker.
 * <p>
 * Sem lógica: o {@code try} existe apenas para controlar o nível do registro. Uma exceção chegando até aqui
 * é defeito, não modo de operação — o relay já isola cada mensagem e nunca deixa uma linha presa em
 * {@code PROCESSING}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OrderBilledMessagingOutboxScheduler {

    private final ProcessOrderBilledMessagingUseCase processOrderBilledMessagingUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.order-billed-messaging.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.order-billed-messaging.fixed-delay}"
    )
    public void relayOrderBilledMessages() {
        try {
            processOrderBilledMessagingUseCase.execute();

        } catch (Exception e) {
            log.error("Unexpected error while relaying the ORDER_BILLED messaging outbox", e);
        }
    }
}
