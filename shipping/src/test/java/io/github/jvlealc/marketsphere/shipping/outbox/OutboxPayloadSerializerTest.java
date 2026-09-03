package io.github.jvlealc.marketsphere.shipping.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderShippedPayload;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * O payload gravado é o contrato publicado <strong>verbatim</strong>: o relay manda ao Kafka exatamente
 * a string que saiu daqui. Um payload que não for objeto JSON quebra o consumidor, e não o produtor, e é
 * por isso que a checagem acontece na gravação, dentro da transação, e não na entrega.
 */
class OutboxPayloadSerializerTest {

    private static final Instant SHIPPED_AT = Instant.parse("2026-09-01T10:00:00Z");
    private static final OrderShippedPayload PAYLOAD =
            new OrderShippedPayload(100L, "BR-2ijs7Su29DaA5", SHIPPED_AT);

    @Test
    void shouldSerializePayloadAsJsonObject() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        SerializedOutboxPayload serialized = new OutboxPayloadSerializer(objectMapper).serialize(PAYLOAD);

        JsonNode json = objectMapper.readTree(serialized.value());
        assertThat(json.isObject()).isTrue();
        assertThat(json.get("orderId").asLong()).isEqualTo(100L);
        assertThat(json.get("trackingCode").asText()).isEqualTo("BR-2ijs7Su29DaA5");
        assertThat(json.has("shippedAt")).isTrue();
    }

    @Test
    void shouldRejectMissingPayload() {
        OutboxPayloadSerializer serializer = new OutboxPayloadSerializer(new ObjectMapper());

        assertThatThrownBy(() -> serializer.serialize(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("payload must not be null");
    }

    /**
     * Com o {@link ObjectMapper} da aplicação, os dois records de payload sempre viram objeto JSON, então
     * esta guarda nunca dispara. Ela existe para o dia em que alguém registrar um serializador próprio
     * para um tipo de payload e ele passar a virar escalar: o corpo da mensagem deixaria de ser objeto e
     * o consumidor quebraria na desserialização. O mapper é dublado porque é a única forma determinística
     * de produzir esse nó sem registrar um módulo Jackson falso no teste.
     */
    @Test
    void shouldRejectPayloadThatIsNotSerializedAsJsonObject() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        when(objectMapper.<JsonNode>valueToTree(PAYLOAD)).thenReturn(TextNode.valueOf("BR-2ijs7Su29DaA5"));

        assertThatThrownBy(() -> new OutboxPayloadSerializer(objectMapper).serialize(PAYLOAD))
                .isInstanceOf(OutboxPayloadSerializationException.class)
                .hasMessageContaining("must be serialized as a JSON object");
    }

    @Test
    void shouldWrapMapperFailure_whenPayloadCannotBeConverted() {
        ObjectMapper objectMapper = mock(ObjectMapper.class);
        IllegalArgumentException mapperFailure = new IllegalArgumentException("no serializer found");
        when(objectMapper.<JsonNode>valueToTree(PAYLOAD)).thenThrow(mapperFailure);

        assertThatThrownBy(() -> new OutboxPayloadSerializer(objectMapper).serialize(PAYLOAD))
                .isInstanceOf(OutboxPayloadSerializationException.class)
                .hasMessageContaining(OrderShippedPayload.class.getName())
                .hasCause(mapperFailure);
    }
}
