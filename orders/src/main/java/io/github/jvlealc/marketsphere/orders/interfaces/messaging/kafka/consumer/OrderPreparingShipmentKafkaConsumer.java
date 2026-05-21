package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderPreparingShipmentCommand;
import io.github.jvlealc.marketsphere.orders.application.usecase.HandleOrderPreparingShipmentUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderPreparingShipmentKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final HandleOrderPreparingShipmentUseCase handleOrderPreparingShipmentUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.config.topics.preparing-shipment-orders}")
    public void consume(String message) {
        OrderPreparingShipmentEvent event = deserialize(message);
        try {
            handleOrderPreparingShipmentUseCase.execute(
                    new HandleOrderPreparingShipmentCommand(event.orderId())
            );
        } catch (Exception e) {
            throw new MessagingConsumptionException("Error consuming ORDER_PREPARING_SHIPMENT event. Order ID: " + event.orderId(), e);
        }
    }

    private OrderPreparingShipmentEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderPreparingShipmentEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_PREPARING_SHIPMENT event from Kafka", e);
        }
    }
}
