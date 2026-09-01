package io.github.jvlealc.marketsphere.shipping.shipment;

import io.github.jvlealc.marketsphere.shipping.outbox.OutboxMessageWriter;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ShipmentPreparationService {

    private static final Logger log = LoggerFactory.getLogger(ShipmentPreparationService.class);

    private final OutboxMessageWriter outboxMessageWriter;
    private final ShipmentRepository shipmentRepository;
    private final Clock clock;
    private final ShipmentEventRepository shipmentEventRepository;

    public ShipmentPreparationService(
            OutboxMessageWriter outboxMessageWriter,
            ShipmentRepository shipmentRepository,
            ShipmentEventRepository shipmentEventRepository,
            Clock clock
    ) {
        this.outboxMessageWriter = outboxMessageWriter;
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.clock = clock;
    }

    /**
     * A linha de outbox é gravada na mesma transação do agregado; publicar é trabalho do relay.
     */
    @Transactional
    public void prepare(
            Long orderId,
            Instant billedAt,
            Long customerId,
            String customerEmail,
            String customerName,
            EventLineage lineage
    ) {
        log.info("Initiating shipment processing for order ID: {}", orderId);

        if (shipmentRepository.existsByOrderId(orderId)) {
            log.info("Shipment already exists for order ID: {}. Ignoring duplicated ORDER_READY_FOR_SHIPMENT event.", orderId);
            return;
        }

        Shipment saved = shipmentRepository.save(
                Shipment.createPreparingShipment(
                        orderId, billedAt, customerId, customerEmail, customerName, lineage.correlationId())
        );
        shipmentEventRepository.save(new ShipmentEvent(saved, "Shipment created from ORDER_READY_FOR_SHIPMENT event"));

        // O instante do fato é o da entrada em preparação, não o da gravação da linha: quem consome
        // o evento não tem acesso ao agregado para descobri-lo.
        outboxMessageWriter.writeOrderPreparingShipment(saved, lineage, Instant.now(clock));
    }
}
