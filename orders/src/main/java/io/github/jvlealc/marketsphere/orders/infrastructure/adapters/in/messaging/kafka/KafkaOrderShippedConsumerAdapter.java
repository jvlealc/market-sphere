package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.usecase.HandleOrderShippedUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderShippedKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderShippedEventMapper orderShippedEventMapper;
    private final HandleOrderShippedUseCase handleOrderShippedUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.config.topics.shipped-orders}")
    public void consume(String message) {
        OrderShippedEvent event = deserialize(message);
        try {
            handleOrderShippedUseCase.execute(orderShippedEventMapper.toCommand(event));
        } catch (Exception e) {
            throw new MessagingConsumptionException("Error consuming ORDER_SHIPPED event. Order ID: " + event.orderId(), e);
        }
    }

    private OrderShippedEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderShippedEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_SHIPPED event from Kafka", e);
        }
    }
}
