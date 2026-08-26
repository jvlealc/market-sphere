package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.scheduler;

import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessOrderPaidNotificationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidNotificationOutboxScheduler {

    private final ProcessOrderPaidNotificationUseCase processOrderPaidNotificationUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.order-paid-email.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.order-paid-email.fixed-delay}"
    )
    public void processOrderPaidNotifications() {
        try {
            processOrderPaidNotificationUseCase.execute();
        } catch (Exception e){
            log.warn("Unexpected error while processing order paid notifications outbox", e);
        }
    }
}
