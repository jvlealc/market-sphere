package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.messaging.kafka;

import io.github.jvlealc.marketsphere.billing.application.exception.OutboxDeliveryException;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OrderBilledPublisherPort;
import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.KafkaTopicsProps;
import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.OutboxRelayProps;
import io.github.jvlealc.marketsphere.billing.infrastructure.messaging.EventHeaders;
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

/**
 * Publica {@code ORDER_BILLED} no tópico de pedidos faturados.
 * <p>
 * O payload vai <em>verbatim</em>: já é o JSON congelado na transação de negócio, e o adapter não tem
 * opinião sobre o conteúdo. Serializar aqui de novo reabriria a porta para o contrato publicado divergir
 * do contrato gravado. To-do metadado (tipo, versão, linhagem) viaja em headers, justamente para não
 * precisar tocar no payload.
 */
@Component
class KafkaOrderBilledPublisherAdapter implements OrderBilledPublisherPort {

    private final Duration publishTimeout;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProps topics;

    KafkaOrderBilledPublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicsProps topics,
            OutboxRelayProps outboxRelayProps
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.publishTimeout = outboxRelayProps.orderBilledMessaging().deliveryTimeout();
    }

    @Override
    public void publish(OutboxMessage message) {
        try {
            kafkaTemplate.send(toProducerRecord(message))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException(failureDescription(message), e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OutboxDeliveryException(failureDescription(message), cause);

        } catch (TimeoutException | KafkaException e) {
            // KafkaException cobre o que estoura de forma síncrona em send() — falha ao obter o producer,
            // buffer cheio, erro de serializer. Sem este ramo, a mesma indisponibilidade do broker
            // apareceria ora classificada, ora como KafkaException crua, dependendo apenas de onde foi
            // percebida.
            //
            // Continuamos NÃO capturando RuntimeException aqui: classificar é papel do adaptador, e um NPE
            // não é modo de falha da tecnologia. O relay tem uma rede de segurança para isso, que registra a
            // ocorrência de forma limitada em vez de deixar a mensagem presa em PROCESSING.
            throw new OutboxDeliveryException(failureDescription(message), e);
        }
    }

    private ProducerRecord<String, String> toProducerRecord(OutboxMessage message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
                topics.billedOrders(),
                messageKeyOf(message),
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

    /**
     * O {@code messageKey} do envelope — hoje o {@code orderId}, a chave de negócio pela qual os eventos de
     * um pedido se ordenam entre si.
     */
    private static String messageKeyOf(OutboxMessage message) {
        return message.getMessageKey();
    }

    /**
     * Header ausente e header com valor nulo não são o mesmo para to-do consumidor. Omitir é o
     * comportamento mais previsível — é o que {@code causation-id} faz quando o evento de entrada não
     * trouxe linhagem.
     */
    private static void putHeader(Headers headers, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }

        headers.add(name, value.getBytes(StandardCharsets.UTF_8));
    }

    private String failureDescription(OutboxMessage message) {
        return "Failed to publish ORDER_BILLED outbox message %s to topic %s"
                .formatted(message.getId(), topics.billedOrders());
    }
}
