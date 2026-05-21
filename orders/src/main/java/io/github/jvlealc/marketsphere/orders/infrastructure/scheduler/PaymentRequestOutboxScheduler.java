package io.github.jvlealc.marketsphere.orders.infrastructure.scheduler;

import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessPaymentRequestUseCase;
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
            initialDelayString = "${market-sphere.outbox.payment-request.initial-delay-ms:5000}",
            fixedDelayString = "${market-sphere.outbox.payment-request.fixed-delay-ms:5000}"
    )
    public void processPaymentRequests() {
        try {
            processPaymentRequestUseCase.execute();
        } catch (Exception e) {
            log.warn("Unexpected error while processing payment requests outbox", e);
        }
    }
}
