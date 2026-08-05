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
     * Recebe o {@link ConsumerRecord} inteiro, e não só o valor, porque a linhagem do evento vem nos
     * headers. Lê-los à mão, decodificando UTF-8, evita depender de como o {@code KafkaHeaderMapper}
     * configurado converte headers desconhecidos — que, dependendo do mapper, chegariam como {@code byte[]}
     * num parâmetro anotado com {@code @Header String}.
     */
    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.config.kafka.topics.paid-orders}"
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
     * <p>
     * Hoje o produtor do {@code orders} publica sem headers, então ambos chegam nulos e
     * {@link EventLineage#from} trata este serviço como raiz do fluxo, gerando o {@code correlationId}.
     * Quando o {@code orders} passar a enviá-los, nada aqui muda.
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
            throw new MessagingDeserializationException("Error deserializing ORDER_PAID event from Kafka", e);
        }
    }
}
