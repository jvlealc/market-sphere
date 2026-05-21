package io.github.jvlealc.marketsphere.shipping.service;

import io.github.jvlealc.marketsphere.shipping.entity.Shipment;
import io.github.jvlealc.marketsphere.shipping.notification.ShipmentConfirmationNotification;
import io.github.jvlealc.marketsphere.shipping.notification.ShipmentEmailSender;
import io.github.jvlealc.marketsphere.shipping.repository.ShipmentRepository;
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
    private final ShipmentEmailSender shipmentEmailSender;

    public ShipmentConfirmationEmailService(ShipmentRepository shipmentRepository, ShipmentEmailSender shipmentEmailSender) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEmailSender = shipmentEmailSender;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendIfNecessary(UUID shipmentId) {
        Shipment shipment = shipmentRepository.findById(shipmentId)
                .orElse(null);

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
            shipment.markShipmentEmailAsSent(Instant.now());
        } catch (Exception e) {
            log.warn("Failed to send shipment confirmation email. Order ID: {}. Error: {}", shipment.getOrderId(), e.getMessage(), e);
        }
    }
}
