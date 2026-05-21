package io.github.jvlealc.marketsphere.shipping.service;

import io.github.jvlealc.marketsphere.shipping.entity.Shipment;
import io.github.jvlealc.marketsphere.shipping.entity.ShipmentEvent;
import io.github.jvlealc.marketsphere.shipping.repository.ShipmentEventRepository;
import io.github.jvlealc.marketsphere.shipping.repository.ShipmentRepository;
import io.github.jvlealc.marketsphere.shipping.event.ShipmentPreparationStartedApplicationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ShipmentPreparationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentPreparationService.class);

    private final ApplicationEventPublisher applicationEventPublisher;
    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;

    public ShipmentPreparationService(
            ApplicationEventPublisher applicationEventPublisher,
            ShipmentRepository shipmentRepository,
            ShipmentEventRepository shipmentEventRepository
    ) {
        this.applicationEventPublisher = applicationEventPublisher;
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
    }

    /**
     * Processa dados de um evento de pedido faturado (ORDER_BILLED), criando um shipment em preparação.
     * Após a persistência do novo shipment e registrar o histórico, publica um evento interno da aplicação.
     * O evento Kafka ORDER_PREPARING_SHIPMENT será publicado após o commit da transação.
     */
    @Transactional
    public void prepare(Long orderId, Instant billedAt, String customerEmail, String customerName) {
        log.info("Initiating shipment processing for order ID: {}", orderId);

        if (shipmentRepository.existsByOrderId(orderId)) {
            log.info("Shipment already exists for order ID: {}. Ignoring duplicated ORDER_BILLED event.", orderId);
            return;
        }

        Shipment saved = shipmentRepository.save(
                Shipment.createPreparingShipment(orderId, billedAt, customerEmail, customerName)
        );
        shipmentEventRepository.save(new ShipmentEvent(saved, "Shipment created from ORDER_BILLED event"));

        applicationEventPublisher.publishEvent(new ShipmentPreparationStartedApplicationEvent(saved.getOrderId()));
    }
}
