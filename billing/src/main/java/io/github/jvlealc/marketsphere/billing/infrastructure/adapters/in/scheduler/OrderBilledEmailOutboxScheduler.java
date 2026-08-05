package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.scheduler;

import io.github.jvlealc.marketsphere.billing.application.usecase.ProcessOrderBilledEmailUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gatilho do relay do canal {@code EMAIL}. Ver {@link OrderBilledMessagingOutboxScheduler} para o desenho.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class OrderBilledEmailOutboxScheduler {

    private final ProcessOrderBilledEmailUseCase processOrderBilledEmailUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.order-billed-email.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.order-billed-email.fixed-delay}"
    )
    public void relayOrderBilledEmails() {
        try {
            processOrderBilledEmailUseCase.execute();

        } catch (Exception e) {
            log.error("Unexpected error while relaying the ORDER_BILLED e-mail outbox", e);
        }
    }
}
