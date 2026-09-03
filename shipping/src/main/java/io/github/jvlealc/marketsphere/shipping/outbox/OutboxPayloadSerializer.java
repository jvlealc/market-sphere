package io.github.jvlealc.marketsphere.shipping.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OutboxPayload;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class OutboxPayloadSerializer {

    private final ObjectMapper objectMapper;

    public OutboxPayloadSerializer(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

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
}
