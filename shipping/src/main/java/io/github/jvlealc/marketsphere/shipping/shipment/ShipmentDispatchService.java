package io.github.jvlealc.marketsphere.shipping.shipment;

import io.github.jvlealc.marketsphere.shipping.shipment.rest.DispatchShipmentRequest;
import io.github.jvlealc.marketsphere.shipping.shipment.rest.InvalidShipmentRequestException;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import io.github.jvlealc.marketsphere.shipping.outbox.OutboxMessageWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Service
public class ShipmentDispatchService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final OutboxMessageWriter outboxMessageWriter;
    private final Clock clock;


    public ShipmentDispatchService(
            ShipmentRepository shipmentRepository,
            ShipmentEventRepository shipmentEventRepository,
            OutboxMessageWriter outboxMessageWriter,
            Clock clock
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.outboxMessageWriter = outboxMessageWriter;
        this.clock = clock;
    }

    @Transactional
    public void dispatch(DispatchShipmentRequest request) {
        validateDispatchRequest(request);

        Shipment shipment = getOrThrow(request.shipmentId(), request.orderId());

        Instant shippedAt = (request.shippedAt() == null)
                ? Instant.now(clock)
                : request.shippedAt();

        boolean markedAsShipped = shipment.markAsShipped(request.trackingCode(), request.carrier(), shippedAt);

        if (!markedAsShipped) {
            return;
        }

        shipmentEventRepository.save(
                new ShipmentEvent(shipment, "Shipment dispatched")
        );

        outboxMessageWriter.writeOrderShipped(shipment, EventLineage.from(shipment.getCorrelationId(), null));
    }

    private static void validateDispatchRequest(DispatchShipmentRequest request) {
        Objects.requireNonNull(request, "Dispatch shipment request is required");

        if (request.shipmentId() == null && request.orderId() == null) {
            throw new InvalidShipmentRequestException("Shipment ID or order ID is required");
        }
    }

    private Shipment getOrThrow(UUID shipmentId, Long orderId) {
        if (shipmentId != null) {
            Shipment shipment = shipmentRepository.findById(shipmentId)
                    .orElseThrow(() -> new ShipmentNotFoundException(shipmentId));

            if (orderId != null &&  !shipment.getOrderId().equals(orderId)) {
                throw new InvalidShipmentRequestException("Shipment ID and order ID do not refer to the same shipment");
            }

            return shipment;
        }

        return shipmentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ShipmentNotFoundException(orderId));
    }
}
