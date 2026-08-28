package io.github.jvlealc.marketsphere.shipping.shipment.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.shipment.ShipmentPreparationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Subscriber que ouve o tópico de pedidos faturados.
 * Responsável por desserializar a mensagem e delegar o processamento
 * para o {@link ShipmentPreparationService}.
 */
@Component
class OrderReadyForShipmentConsumer {

    private final ObjectMapper objectMapper;
    private final ShipmentPreparationService shipmentPreparationService;

    OrderReadyForShipmentConsumer(
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
    void listen(String jsonMessage) {
        OrderReadyForShipmentEvent event = deserialize(jsonMessage);
        try {
            shipmentPreparationService.prepare(
                    event.orderId(),
                    event.billedAt(),
                    event.customer().customerId(),
                    event.customer().email(),
                    event.customer().fullName()
            );
        } catch (Exception e) {
            throw new MessagingConsumptionException("Error consuming ORDER_BILLED message. Order ID: " + event.orderId(), e);
        }
    }

    private OrderReadyForShipmentEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderReadyForShipmentEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_BILLED event from Kafka", e);
        }
    }
}
