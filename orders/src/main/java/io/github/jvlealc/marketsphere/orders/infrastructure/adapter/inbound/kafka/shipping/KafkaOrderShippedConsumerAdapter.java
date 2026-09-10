package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.shipping;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.order.shipping.HandleOrderShippedUseCase;
import io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.KafkaEventLineageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderShippedConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final OrderShippedEventMapper orderShippedEventMapper;
    private final HandleOrderShippedUseCase handleOrderShippedUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.topics.shipped-orders}")
    public void consume(ConsumerRecord<String, String> record) {
        EventLineage eventLineage = KafkaEventLineageMapper.nextEventLineageFrom(record);

        log.info("Received OrderShippedEvent message. correlationId={}, causationId={}.",
                eventLineage.correlationId(), eventLineage.causationId());

        OrderShippedEvent event = deserialize(record.value());

        handleOrderShippedUseCase.execute(orderShippedEventMapper.toCommand(event));
    }

    private OrderShippedEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderShippedEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_SHIPPED payload from Kafka", e);
        }
    }
}
