package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.messaging.kafka;

import io.github.jvlealc.marketsphere.orders.application.exception.OutboxDeliveryException;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.KafkaTopicsProps;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.OutboxRelayProps;
import io.github.jvlealc.marketsphere.orders.infrastructure.messaging.EventHeaders;
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
public class KafkaOrderPaidPublisherAdapter implements OrderPaidPublisherPort {

    private final Duration publishTimeout;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaTopicsProps topics;

    public KafkaOrderPaidPublisherAdapter(
            KafkaTemplate<String, String> kafkaTemplate,
            KafkaTopicsProps topics,
            OutboxRelayProps props
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topics = topics;
        this.publishTimeout = props.orderPaidMessaging().deliveryTimeout();
    }

    @Override
    public void publish(OutboxMessage message) {
        validateMessage(message);

        try {
            kafkaTemplate.send(toProducerRecord(message))
                    .get(publishTimeout.toMillis(), TimeUnit.MILLISECONDS);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException(failureDescription(message), e);

        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null
                    ? e.getCause()
                    : e;

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
                topics.paidOrders(),
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

    private String failureDescription(OutboxMessage message) {
        return "Failed to publish ORDER_PAID outbox message %s to topic %s"
                .formatted(message.getId(), topics.paidOrders());
    }

    private static void validateMessage(OutboxMessage message) {
        if (message.getEventType() != OutboxEventType.ORDER_PAID) {
            throw new IllegalArgumentException("KafkaOrderPaidPublisherAdapter only accepts ORDER_PAID messages");
        }

        if (message.getChannel() != OutboxChannel.MESSAGING) {
            throw new IllegalArgumentException("KafkaOrderPaidPublisherAdapter only accepts MESSAGING messages");
        }
    }

    private static void putHeader(Headers headers, String key, String value) {
        if (value == null || value.isBlank()) return;
        headers.add(key, value.getBytes(StandardCharsets.UTF_8));
    }
}
