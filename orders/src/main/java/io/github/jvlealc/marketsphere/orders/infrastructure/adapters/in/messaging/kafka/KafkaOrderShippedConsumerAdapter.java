package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.usecase.HandleOrderShippedUseCase;
import io.github.jvlealc.marketsphere.orders.infrastructure.messaging.EventHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderShippedConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final OrderShippedEventMapper orderShippedEventMapper;
    private final HandleOrderShippedUseCase handleOrderShippedUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.topics.shipped-orders}")
    public void consume(ConsumerRecord<String, String> record) {
        EventLineage eventLineage = toEventLineage(record);

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

    private static EventLineage toEventLineage(ConsumerRecord<String, String> record) {
        return EventLineage.from(
                headerValue(record, EventHeaders.CORRELATION_ID),
                headerValue(record, EventHeaders.CAUSATION_ID)
        );
    }

    private static String headerValue(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);

        if (header == null || header.value() == null) return null;

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
