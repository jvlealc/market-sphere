package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.scheduling;

import io.github.jvlealc.marketsphere.orders.application.outbox.relay.ProcessPaymentRequestUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
final class PaymentRequestOutboxScheduler {

    private final ProcessPaymentRequestUseCase processPaymentRequestUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.payment-request.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.payment-request.fixed-delay}"
    )
    public void processPaymentRequests() {
        try {
            processPaymentRequestUseCase.execute();
        } catch (Exception e) {
            log.warn("Unexpected error while processing payment requests outbox", e);
        }
    }
}
