package io.github.jvlealc.marketsphere.orders.interfaces.messaging.kafka.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.usecase.HandleOrderBilledUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderBilledKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final OrderBilledEventMapper orderBilledEventMapper;
    private final HandleOrderBilledUseCase handleOrderBilledUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.config.topics.billed-orders}")
    public void consume(String message) {
        OrderBilledEvent event = deserialize(message);
        try {
            handleOrderBilledUseCase.execute(orderBilledEventMapper.toCommand(event));
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
