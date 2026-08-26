package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.scheduler;

import io.github.jvlealc.marketsphere.orders.application.usecase.ProcessOrderReadyForShipmentUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderReadyForShipmentOutboxScheduler {

    private final ProcessOrderReadyForShipmentUseCase processOrderReadyForShipmentUseCase;

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.order-ready-for-shipment.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.order-ready-for-shipment.fixed-delay}"
    )
    public void processOrderReadyForShipmentEvents() {
        try {
            processOrderReadyForShipmentUseCase.execute();
        } catch (Exception e) {
            log.warn("Unexpected error while processing order ready for shipment events outbox", e);
        }
    }
}
