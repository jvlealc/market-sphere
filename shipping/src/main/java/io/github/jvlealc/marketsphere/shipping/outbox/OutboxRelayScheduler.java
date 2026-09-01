package io.github.jvlealc.marketsphere.shipping.outbox;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Usa {@code fixedDelay}, que conta a partir do fim da execução anterior: com {@code fixedRate}, um
 * lote lento sobreporia o próximo tick e criaria dois relays concorrentes na mesma instância.
 */
@Component
class OutboxRelayScheduler {

    private final OutboxRelayService relayService;

    OutboxRelayScheduler(OutboxRelayService relayService) {
        this.relayService = relayService;
    }

    @Scheduled(
            initialDelayString = "${market-sphere.outbox.relay.initial-delay}",
            fixedDelayString = "${market-sphere.outbox.relay.fixed-delay}"
    )
    void relay() {
        relayService.relayPendingMessages();
    }
}
