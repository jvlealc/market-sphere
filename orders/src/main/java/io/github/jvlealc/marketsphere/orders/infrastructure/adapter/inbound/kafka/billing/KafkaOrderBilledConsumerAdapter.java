package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka.billing;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.order.billing.HandleOrderBilledUseCase;
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
class KafkaOrderBilledConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final OrderBilledEventMapper orderBilledEventMapper;
    private final HandleOrderBilledUseCase handleOrderBilledUseCase;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.kafka.topics.billed-orders}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        EventLineage eventLineage = KafkaEventLineageMapper.nextEventLineageFrom(record);

        log.info("Received OrderBilledEvent message. correlationId={}, causationId={}.",
                eventLineage.correlationId(), eventLineage.causationId());

        OrderBilledEvent event = deserialize(record.value());

        handleOrderBilledUseCase.execute(orderBilledEventMapper.toCommand(event), eventLineage);
    }

    private OrderBilledEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderBilledEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_BILLED payload from Kafka", e);
        }
    }
}
