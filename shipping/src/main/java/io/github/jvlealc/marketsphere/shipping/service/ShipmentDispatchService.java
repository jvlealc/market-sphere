package io.github.jvlealc.marketsphere.shipping.service;

import io.github.jvlealc.marketsphere.shipping.dto.DispatchShipmentRequest;
import io.github.jvlealc.marketsphere.shipping.entity.Shipment;
import io.github.jvlealc.marketsphere.shipping.entity.ShipmentEvent;
import io.github.jvlealc.marketsphere.shipping.exception.InvalidShipmentRequestException;
import io.github.jvlealc.marketsphere.shipping.exception.ShipmentNotFoundException;
import io.github.jvlealc.marketsphere.shipping.repository.ShipmentEventRepository;
import io.github.jvlealc.marketsphere.shipping.repository.ShipmentRepository;
import io.github.jvlealc.marketsphere.shipping.event.ShipmentDispatchedApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ShipmentDispatchService {

    private final ShipmentRepository shipmentRepository;
    private final ShipmentEventRepository shipmentEventRepository;
    private final ApplicationEventPublisher applicationEventPublisher;


    public ShipmentDispatchService(
            ShipmentRepository shipmentRepository,
            ShipmentEventRepository shipmentEventRepository,
            ApplicationEventPublisher applicationEventPublisher
    ) {
        this.shipmentRepository = shipmentRepository;
        this.shipmentEventRepository = shipmentEventRepository;
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @Transactional
    public void dispatch(DispatchShipmentRequest request) {
        validateDispatchRequest(request);

        Shipment shipment = getOrThrow(request.shipmentId(), request.orderId());

        Instant shippedAt = (request.shippedAt() == null)
                ? Instant.now()
                : request.shippedAt();

        boolean markedAsShipped = shipment.markAsShipped(request.trackingCode(), request.carrier(), shippedAt);

        if (!markedAsShipped) {
            return;
        }

        shipmentEventRepository.save(
                new ShipmentEvent(shipment, "Shipment dispatched")
        );

        applicationEventPublisher.publishEvent(
                new ShipmentDispatchedApplicationEvent(
                        shipment.getId(),
                        shipment.getOrderId(),
                        shipment.getTrackingCode(),
                        shipment.getShippedAt()
                )
        );
    }

    private static void validateDispatchRequest(DispatchShipmentRequest request) {
        if (request == null) {
            throw new InvalidShipmentRequestException("Dispatch shipment request is required");
        }

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
