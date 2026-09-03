package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.serialization.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.SerializedOutboxPayload;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.payload.OutboxPayload;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxPayloadCodecPort;
import io.github.jvlealc.marketsphere.orders.application.exception.OutboxPayloadDeserializationException;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.OutboxPayloadSerializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Serializa e desserializa o payload da outbox com Jackson 2.
 */
@Component
@RequiredArgsConstructor
class JacksonOutboxPayloadCodecAdapter implements OutboxPayloadCodecPort {

    private final ObjectMapper objectMapper;

    @Override
    public SerializedOutboxPayload serialize(OutboxPayload payload) {
        Objects.requireNonNull(payload, "payload must not be null");

        try {
            JsonNode jsonNode = objectMapper.valueToTree(payload);

            if (jsonNode == null || !jsonNode.isObject()) {
                throw new OutboxPayloadSerializationException("Outbox payload must be serialized as a JSON object");
            }

            return new SerializedOutboxPayload(jsonNode.toString());

        } catch (IllegalArgumentException e) {
            throw new OutboxPayloadSerializationException(
                    "Could not serialize outbox payload of type: " + payload.getClass().getName(),
                    e
            );
        }
    }

    @Override
    public <T extends OutboxPayload> T deserialize(SerializedOutboxPayload payload, Class<T> type) {
        Objects.requireNonNull(payload, "payload must not be null");
        Objects.requireNonNull(type, "type must not be null");

        try {
            return objectMapper.readValue(payload.value(), type);

        } catch (JsonProcessingException e) {
            throw new OutboxPayloadDeserializationException(
                    "Could not deserialize outbox payload into type: " + type.getName(),
                    e
            );
        }
    }
}
