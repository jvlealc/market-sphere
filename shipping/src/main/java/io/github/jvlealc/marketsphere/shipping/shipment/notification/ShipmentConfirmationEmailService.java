package io.github.jvlealc.marketsphere.shipping.shipment.notification;

import io.github.jvlealc.marketsphere.shipping.shipment.Shipment;
import io.github.jvlealc.marketsphere.shipping.shipment.ShipmentJpaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ShipmentConfirmationEmailService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentConfirmationEmailService.class);

    private final ShipmentJpaRepository shipmentJpaRepository;
    private final ShipmentEmailSender shipmentEmailSender;
    private final ShipmentEmailProps props;
    private final Clock clock;

    public ShipmentConfirmationEmailService(
            ShipmentJpaRepository shipmentJpaRepository,
            ShipmentEmailSender shipmentEmailSender,
            ShipmentEmailProps props,
            Clock clock
    ) {
        this.shipmentJpaRepository = shipmentJpaRepository;
        this.shipmentEmailSender = shipmentEmailSender;
        this.props = props;
        this.clock = clock;
    }

    /**
     * Devolve os ids em vez de iterar aqui: chamar {@code sendIfNecessary} de dentro desta classe seria
     * auto-invocacao, o proxy do Spring nao seria atravessado e o {@code REQUIRES_NEW} nao valeria —
     * as marcacoes de envio nunca chegariam ao banco.
     */
    public List<UUID> findPendingConfirmationIds() {
        return shipmentJpaRepository.findPendingConfirmationEmails(
                        Instant.now(clock),
                        props.maxAttempts(),
                        Limit.of(props.batchSize()))
                .stream()
                .map(Shipment::getId)
                .toList();
    }

    /**
     * Transação propria por remessa: os dois ramos precisam commitar. Se a exceção
     * escapasse, o incremento do contador faria rollback junto e a varredura retentaria para sempre.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfNecessary(UUID shipmentId) {
        Shipment shipment = shipmentJpaRepository.findById(shipmentId).orElse(null);

        if (shipment == null) return;
        if (shipment.getShipmentEmailSentAt() != null) return;

        try {
            shipmentEmailSender.sendShipmentConfirmation(new ShipmentConfirmationNotification(
                    shipment.getOrderId(),
                    shipment.getCustomerName(),
                    shipment.getCustomerEmail(),
                    shipment.getTrackingCode(),
                    shipment.getShippedAt()
            ));

            shipment.markShipmentEmailAsSent(Instant.now(clock));

        } catch (ShipmentEmailDeliveryException e) {
            int attemptsSoFar = shipment.getShipmentEmailAttempts();
            Instant nextAttemptAt = Instant.now(clock).plus(props.backoffFor(attemptsSoFar));

            shipment.registerEmailDeliveryFailure(nextAttemptAt);

            if (attemptsSoFar + 1 >= props.maxAttempts()) {
                log.error("Shipment confirmation e-mail exhausted its attempts. orderId={}, attempts={}",
                        shipment.getOrderId(), attemptsSoFar + 1, e);
            } else {
                log.warn("Failed to send shipment confirmation e-mail. orderId={}, attempt={}, nextAttemptAt={}",
                        shipment.getOrderId(), attemptsSoFar + 1, nextAttemptAt, e);
            }
        }
    }
}
