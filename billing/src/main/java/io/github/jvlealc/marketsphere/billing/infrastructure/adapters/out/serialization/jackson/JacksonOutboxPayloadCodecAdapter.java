package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxPayload;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.payload.OutboxPayloadData;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.OutboxPayloadDeserializationException;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.OutboxPayloadSerializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static java.util.Objects.requireNonNull;

/**
 * Serializa e desserializa o payload da outbox com Jackson.
 */
@Component
@RequiredArgsConstructor
class JacksonOutboxPayloadCodecAdapter implements OutboxPayloadCodecPort {

    private final ObjectMapper objectMapper;

    @Override
    public OutboxPayload serialize(OutboxPayloadData payload) {
        requireNonNull(payload, "Outbox event data must not be null");

        try {
            JsonNode jsonNode = objectMapper.valueToTree(payload);

            if (jsonNode == null || !jsonNode.isObject()) {
                throw new OutboxPayloadSerializationException("Outbox payload must be serialized as a JSON object");
            }

            return new OutboxPayload(jsonNode.toString());

        } catch (IllegalArgumentException e) {
            throw new OutboxPayloadSerializationException(
                    "Could not serialize outbox payload of type: " + payload.getClass().getName(), e
            );
        }
    }

    @Override
    public <T extends OutboxPayloadData> T deserialize(OutboxPayload payload, Class<T> type) {
        requireNonNull(payload, "Outbox payload must not be null");
        requireNonNull(type, "Target payload type must not be null");

        try {
            return objectMapper.readValue(payload.value(), type);

        } catch (JsonProcessingException e) {
            throw new OutboxPayloadDeserializationException(
                    "Could not deserialize outbox payload into type: " + type.getName(), e
            );
        }
    }
}
