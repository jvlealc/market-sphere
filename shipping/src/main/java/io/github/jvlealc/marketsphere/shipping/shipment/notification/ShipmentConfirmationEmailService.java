package io.github.jvlealc.marketsphere.shipping.shipment.notification;

import io.github.jvlealc.marketsphere.shipping.shipment.Shipment;
import io.github.jvlealc.marketsphere.shipping.shipment.ShipmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ShipmentConfirmationEmailService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentConfirmationEmailService.class);

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEmailSender emailSender;

    public ShipmentConfirmationEmailService(ShipmentRepository shipmentRepository, ShipmentEmailSender emailSender) {
        this.shipmentRepository = shipmentRepository;
        this.emailSender = emailSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfNecessary(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElse(null);

        if (shipment == null) return;
        if (shipment.getShipmentEmailSentAt() != null) return;

        try {
            emailSender.sendShipmentConfirmation(new ShipmentConfirmationNotification(
                    shipment.getOrderId(),
                    shipment.getCustomerName(),
                    shipment.getCustomerEmail(),
                    shipment.getTrackingCode(),
                    shipment.getShippedAt()
            ));

            shipment.markShipmentEmailAsSent(Instant.now());

        } catch (ShipmentEmailDeliveryException e) {
            shipment.registerEmailDeliveryFailure();

            log.warn("Failed to send shipment confirmation e-mail. Order ID: {}. Error: {}", shipment.getOrderId(), e.getMessage(), e);
        }
    }
}
