package io.github.jvlealc.marketsphere.shipping.shipment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import io.github.jvlealc.marketsphere.shipping.messaging.kafka.EventHeaderReader;
import io.github.jvlealc.marketsphere.shipping.shipment.ShipmentPreparationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
class KafkaOrderReadyForShipmentConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaOrderReadyForShipmentConsumer.class);

    private final ObjectMapper objectMapper;
    private final ShipmentPreparationService shipmentPreparationService;

    KafkaOrderReadyForShipmentConsumer(
            ObjectMapper objectMapper,
            ShipmentPreparationService shipmentPreparationService
    ) {
        this.objectMapper = objectMapper;
        this.shipmentPreparationService = shipmentPreparationService;
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.kafka.topics.ready-for-shipment-orders}"
    )
    void listen(ConsumerRecord<String, String> record) {

        EventLineage eventLineage = EventHeaderReader.nextEventLineageFrom(record);

        log.info("Received OrderReadyForShipmentEvent message. correlationId={}, causedBy={}.",
                eventLineage.correlationId(), eventLineage.causationId());

        OrderReadyForShipmentEvent event = deserialize(record.value());

        shipmentPreparationService.prepare(
                event.orderId(),
                event.billedAt(),
                event.customer().customerId(),
                event.customer().email(),
                event.customer().fullName(),
                eventLineage
        );
    }

    private OrderReadyForShipmentEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderReadyForShipmentEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_READY_FOR_SHIPMENT event from Kafka", e);
        }
    }
}
