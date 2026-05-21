package io.github.jvlealc.marketsphere.shipping.messaging.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingConsumptionException;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.shipping.service.ShipmentPreparationService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Subscriber que ouve o tópico de pedidos faturados.
 * Responsável por desserializar a mensagem e delegar o processamento
 * para o {@link ShipmentPreparationService}.
 */
@Component
public class OrderBilledListener {

    private final ObjectMapper objectMapper;
    private final ShipmentPreparationService shipmentPreparationService;

    public OrderBilledListener(
            ObjectMapper objectMapper,
            ShipmentPreparationService shipmentPreparationService
    ) {
        this.objectMapper = objectMapper;
        this.shipmentPreparationService = shipmentPreparationService;
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.kafka.config.topics.billed-orders}"
    )
    public void listen(String jsonMessage) {
        OrderBilledEvent event = deserialize(jsonMessage);
        try {
            shipmentPreparationService.prepare(
                    event.orderId(),
                    event.billedAt(),
                    event.customer().email(),
                    event.customer().fullName()
            );
        } catch (Exception e) {
            throw new MessagingConsumptionException("Error consuming ORDER_BILLED message. Order ID: " + event.orderId(), e);
        }
    }

    private OrderBilledEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderBilledEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_BILLED event from Kafka", e);
        }
    }
}
