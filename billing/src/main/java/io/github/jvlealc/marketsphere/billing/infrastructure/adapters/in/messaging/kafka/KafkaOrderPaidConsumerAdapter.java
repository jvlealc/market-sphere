package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.in.messaging.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.usecase.HandleOrderPaidUseCase;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.billing.infrastructure.messaging.EventHeaders;
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
class KafkaOrderPaidConsumerAdapter {

    private final ObjectMapper objectMapper;
    private final OrderPaidEventMapper orderPaidEventMapper;
    private final HandleOrderPaidUseCase handleOrderPaidUseCase;

    /**
     * Recebe o {@link ConsumerRecord} inteiro porque a linhagem vem nos headers, e lê-los à mão evita
     * o {@code byte[]} que um {@code @Header String} recebe dependendo do {@code KafkaHeaderMapper}.
     */
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.kafka.topics.paid-orders}"
    )
    public void consume(ConsumerRecord<String, String> record) {
        EventLineage eventLineage = toEventLineage(record);

        log.info("Received OrderPaidEvent message. correlationId={}, causationId={}. Starting billing processing...",
                eventLineage.correlationId(), eventLineage.causationId());

        OrderPaidEvent event = deserialize(record.value());

        handleOrderPaidUseCase.execute(orderPaidEventMapper.toApplicationModel(event), eventLineage);
    }

    /**
     * O {@code event-id} do {@code ORDER_PAID} vira o {@code causationId} de tudo que este serviço publicar
     * a partir dele — é o que amarra a cadeia causal entre serviços.
     */
    private static EventLineage toEventLineage(ConsumerRecord<String, String> record) {
        return EventLineage.from(
                headerValue(record, EventHeaders.CORRELATION_ID),
                headerValue(record, EventHeaders.EVENT_ID)
        );
    }

    private static String headerValue(ConsumerRecord<String, String> record, String name) {
        Header header = record.headers().lastHeader(name);

        if (header == null || header.value() == null) {
            return null;
        }

        return new String(header.value(), StandardCharsets.UTF_8);
    }

    private OrderPaidEvent deserialize(String message) {
        try {
            return objectMapper.readValue(message, OrderPaidEvent.class);
        } catch (JsonProcessingException e) {
            throw new MessagingDeserializationException("Error deserializing ORDER_PAID payload from Kafka", e);
        }
    }
}
