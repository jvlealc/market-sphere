package io.github.jvlealc.marketsphere.orders.infrastructure.scheduler;

import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessOrderPaidMessagingUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaidMessagingOutboxScheduler {

    private final ProcessOrderPaidMessagingUseCase processOrderPaidMessagingUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.order-paid-messaging.initial-delay-ms:7000}",
            fixedDelayString = "${market-sphere.outbox.order-paid-messaging.fixed-delay-ms:5000}"
    )
    public void processOrderPaidEvents() {
        try {
            processOrderPaidMessagingUseCase.execute();
        } catch (Exception e){
            log.warn("Unexpected error while processing order paid events outbox", e);
        }
    }
}
