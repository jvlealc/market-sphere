package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.SerializedOutboxPayload;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.OutboxPayloadMappingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class OutboxMessageJpaEntityMapper {

    private final ObjectMapper objectMapper;

    public OutboxMessageJpaEntity toEntity(OutboxMessage message) {
        requireNonNull(message, "Outbox message must not be null");
        EventLineage eventLineage = requireNonNull(message.getEventLineage(), "Event lineage must not be null");

        return new OutboxMessageJpaEntity(
                message.getId(),
                message.getAggregateType(),
                message.getAggregateId(),
                message.getEventType(),
                message.getEventVersion(),
                message.getOccurredAt(),
                message.getChannel(),
                message.getMessageKey(),
                eventLineage.correlationId(),
                eventLineage.causationId(),
                toJsonNode(message.getPayload()),
                message.getStatus(),
                message.getAttempts(),
                message.getMaxAttempts(),
                message.getNextAttemptAt(),
                message.getIdempotencyKey(),
                toFailureReasonStr(message.getFailureReason())
        );
    }

    public OutboxMessage toApplicationModel(OutboxMessageJpaEntity entity) {
        requireNonNull(entity, "Outbox message entity must not be null");

        return OutboxMessage.rehydrate(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getEventVersion(),
                entity.getOccurredAt(),
                entity.getChannel(),
                entity.getMessageKey(),
                toOutboxPayload(entity.getPayload()),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getMaxAttempts(),
                entity.getNextAttemptAt(),
                entity.getIdempotencyKey(),
                new EventLineage(entity.getCorrelationId(), entity.getCausationId()),
                toFailureReason(entity.getFailureReason())
        );
    }

    private JsonNode toJsonNode(SerializedOutboxPayload payload) {
        requireNonNull(payload, "Outbox payload must not be null");

        try {
            JsonNode jsonNode = objectMapper.readTree(payload.value());

            if (!jsonNode.isObject()) {
                throw new OutboxPayloadMappingException("Outbox payload must be a JSON object");
            }

            return jsonNode;

        } catch (JsonProcessingException e) {
            throw new OutboxPayloadMappingException("Failed to parse outbox payload as JSON", e);
        }
    }

    private static SerializedOutboxPayload toOutboxPayload(JsonNode jsonNode) {
        if (jsonNode == null) {
            throw new OutboxPayloadMappingException("Persisted outbox payload must not be null");
        }

        if (!jsonNode.isObject()) {
            throw new OutboxPayloadMappingException("Persisted outbox payload must be a JSON object");
        }

        return new SerializedOutboxPayload(jsonNode.toString());
    }

    private static String toFailureReasonStr(OutboxFailureReason failureReason) {
        return failureReason == null
                ? null
                : failureReason.value();
    }

    private static OutboxFailureReason toFailureReason(String persistedFailureReason) {
        return persistedFailureReason == null
                ? null
                : OutboxFailureReason.of(persistedFailureReason);
    }
}
