package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.messaging.kafka;

import io.github.jvlealc.marketsphere.orders.application.exception.OutboxDeliveryException;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.infrastructure.messaging.EventHeaders;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.header.Headers;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
class KafkaOutboxPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    void publish(OutboxMessage message, OutboxEventType expectedEventType, String topic, Duration publishTimeout) {
        validateMessage(message, expectedEventType);

        try {
            kafkaTemplate.send(toProducerRecord(message, topic))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException(failureDescription(message, topic), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null
                    ? e.getCause()
                    : e;

            throw new OutboxDeliveryException(failureDescription(message, topic), cause);

        } catch (TimeoutException | KafkaException e) {
            // KafkaException cobre o que estoura de forma síncrona em send(): falha ao obter o producer,
            // buffer cheio, erro de serializer.
            throw new OutboxDeliveryException(failureDescription(message, topic), e);
        }
    }

    private static ProducerRecord<String, String> toProducerRecord(OutboxMessage message, String topic) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topic,
                message.getMessageKey(),
                message.getPayload().value()
        );

        EventLineage eventLineage = message.getEventLineage();
        Headers headers = record.headers();

        putHeader(headers, EventHeaders.EVENT_ID, message.getId().toString());
        putHeader(headers, EventHeaders.EVENT_TYPE, message.getEventType().name());
        putHeader(headers, EventHeaders.EVENT_VERSION, String.valueOf(message.getEventVersion()));
        putHeader(headers, EventHeaders.AGGREGATE_TYPE, message.getAggregateType().name());
        putHeader(headers, EventHeaders.AGGREGATE_ID, message.getAggregateId());
        putHeader(headers, EventHeaders.OCCURRED_AT, message.getOccurredAt().toString());
        putHeader(headers, EventHeaders.CORRELATION_ID, eventLineage.correlationId());
        putHeader(headers, EventHeaders.CAUSATION_ID, eventLineage.causationId());
        putHeader(headers, EventHeaders.CONTENT_TYPE, EventHeaders.APPLICATION_JSON);

        return record;
    }

    private static String failureDescription(OutboxMessage message, String topic) {
        return "Failed to publish %s outbox message %s to topic %s"
                .formatted(message.getEventType(), message.getId(), topic);
    }

    private static void validateMessage(OutboxMessage message, OutboxEventType expectedEventType) {
        if (message.getEventType() != expectedEventType) {
            throw new IllegalArgumentException(
                    "Expected %s message, got %s".formatted(expectedEventType, message.getEventType())
            );
        }

        if (message.getChannel() != OutboxChannel.MESSAGING) {
            throw new IllegalArgumentException(
                    "Expected MESSAGING message, got %s".formatted(message.getChannel())
            );
        }
    }

    private static void putHeader(Headers headers, String key, String value) {
        if (value == null || value.isBlank()) return;

        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
