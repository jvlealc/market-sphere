package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.scheduler;

import io.github.jvlealc.marketsphere.billing.application.usecase.ProcessOrderBilledMessagingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gatilho do relay do canal {@code MESSAGING}.
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
