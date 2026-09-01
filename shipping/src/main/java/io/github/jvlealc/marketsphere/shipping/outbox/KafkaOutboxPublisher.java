package io.github.jvlealc.marketsphere.shipping.outbox;

import io.github.jvlealc.marketsphere.shipping.messaging.EventHeaders;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import io.github.jvlealc.marketsphere.shipping.messaging.kafka.KafkaTopicsProps;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Publica a mensagem da outbox com o payload <strong>verbatim</strong> no corpo e o envelope nos
 * headers.
 */
@Component
class KafkaOutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaOutboxPublisher.class);

    private static final String AGGREGATE_TYPE = "SHIPMENT";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProps topics;

    KafkaOutboxPublisher(KafkaTemplate<String, String> kafkaTemplate, KafkaTopicsProps topics) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
    }

    void publish(OutboxMessage message, Duration deliveryTimeout) {
        String topic = topicFor(message.getEventType());

        try {
            kafkaTemplate.send(toProducerRecord(message, topic))
                    .get(deliveryTimeout.toMillis(), TimeUnit.MILLISECONDS);

            log.info("{} published successfully. Topic: {}, messageKey: {}, eventId: {}, correlationId: {}.",
                    message.getEventType(), topic, message.getMessageKey(), message.getId(),
                    message.getEventLineage().correlationId());

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException(failureDescription(message, topic), e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OutboxDeliveryException(failureDescription(message, topic), cause);

        } catch (TimeoutException | KafkaException e) {
            // KafkaException cobre o que estoura de forma síncrona em send(): falha ao obter o producer,
            // buffer cheio, erro de serializer.
            throw new OutboxDeliveryException(failureDescription(message, topic), e);
        }
    }

    private String topicFor(OutboxEventType eventType) {
        return switch (eventType) {
            case ORDER_PREPARING_SHIPMENT -> topics.preparingShipmentOrders();
            case ORDER_SHIPPED -> topics.shippedOrders();
        };
    }

    private static ProducerRecord<String, String> toProducerRecord(OutboxMessage message, String topic) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                message.getMessageKey(),
                message.getPayload().value()
        );

        EventLineage lineage = message.getEventLineage();
        Headers headers = record.headers();

        addHeader(headers, EventHeaders.EVENT_ID, message.getId().toString());
        addHeader(headers, EventHeaders.EVENT_TYPE, message.getEventType().name());
        addHeader(headers, EventHeaders.EVENT_VERSION, String.valueOf(message.getEventVersion()));
        addHeader(headers, EventHeaders.AGGREGATE_TYPE, AGGREGATE_TYPE);
        addHeader(headers, EventHeaders.AGGREGATE_ID, message.getAggregateId());
        addHeader(headers, EventHeaders.OCCURRED_AT, message.getOccurredAt().toString());
        addHeader(headers, EventHeaders.CORRELATION_ID, lineage.correlationId());
        addHeader(headers, EventHeaders.CAUSATION_ID, lineage.causationId());
        addHeader(headers, EventHeaders.CONTENT_TYPE, EventHeaders.APPLICATION_JSON);

        return record;
    }

    private static void addHeader(Headers headers, String key, String value) {
        if (value == null) {
            return;
        }

        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }

    private static String failureDescription(OutboxMessage message, String topic) {
        return "Could not publish outbox message %s (%s) to topic %s".formatted(
                message.getId(), message.getEventType(), topic);
    }
}
