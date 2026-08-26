package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.command.HandleOrderPreparingShipmentCommand;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.usecase.HandleOrderPreparingShipmentUseCase;
import io.github.jvlealc.marketsphere.orders.infrastructure.messaging.EventHeaderReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderPreparingShipmentConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final HandleOrderPreparingShipmentUseCase handleOrderPreparingShipmentUseCase;

    @KafkaListener(topics = "${market-sphere.kafka.topics.preparing-shipment-orders}")
    public void consume(ConsumerRecord<String, String> record) {
        EventLineage eventLineage = EventHeaderReader.readEventLineageFrom(record);

        log.info("Received OrderPreparingShipmentEvent message. correlationId={}, causationId={}.",
                eventLineage.correlationId(), eventLineage.causationId());

        OrderPreparingShipmentEvent event = deserialize(record.value());

        handleOrderPreparingShipmentUseCase.execute(
               new HandleOrderPreparingShipmentCommand(event.orderId())
        );
    }

    private OrderPreparingShipmentEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderPreparingShipmentEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_PREPARING_SHIPMENT payload from Kafka", e);
        }
    }
}
