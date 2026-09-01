package io.github.jvlealc.marketsphere.shipping.shipment;

import io.github.jvlealc.marketsphere.shipping.shipment.notification.ShipmentConfirmationEmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * O e-mail de confirmacao nao passa pela outbox: e um efeito so, com conteudo derivavel do agregado e
 * sem ordenacao a preservar, entao o marcador no proprio {@code Shipment} basta e a consulta e a fila.
 * <p>
 * O espacamento e deliberadamente muito maior que o do relay de eventos: o destinatario e uma pessoa,
 * que tem o site para consultar, enquanto um evento nao entregue trava outro servico.
 */
@Component
class ShipmentEmailScheduler {

    private final ShipmentConfirmationEmailService emailService;

    ShipmentEmailScheduler(ShipmentConfirmationEmailService emailService) {
        this.emailService = emailService;
    }

    @Scheduled(
            initialDelayString = "${market-sphere.shipment.email.initial-delay}",
            fixedDelayString = "${market-sphere.shipment.email.fixed-delay}"
    )
    void sendPendingConfirmations() {
        for (UUID shipmentId : emailService.findPendingConfirmationIds()) {
            emailService.sendIfNecessary(shipmentId);
        }
    }
}
