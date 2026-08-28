package io.github.jvlealc.marketsphere.shipping.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.outbox.payload.OrderShippedPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Kafka Publisher responsável por serializar e publicar o evento
 * {@link OrderShippedPayload} no tópico de pedidos enviados.
 */
@Component
public class OrderShippedPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderShippedPublisher.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OrderShippedPublisher(
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${market-sphere.kafka.topics.shipped-orders}") String topic
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderShippedPayload event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            String messageKey = String.valueOf(event.orderId());

            kafkaTemplate.send(topic, messageKey, jsonPayload).get();

            log.info(
                    "ORDER_SHIPPED event published successfully. Order ID: {}, trackingCode: {}",
                    event.orderId(),
                    event.trackingCode()
            );

        } catch (JsonProcessingException e) {
            throw new UndeliverableOutboxMessageException("Error serializing ORDER_SHIPPED event to Kafka. Order ID: " + event.orderId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OutboxDeliveryException("Thread interrupted while publishing ORDER_SHIPPED event to Kafka. Order ID: " + event.orderId(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new OutboxDeliveryException("Error publishing ORDER_SHIPPED event to Kafka. Order ID: " + event.orderId(), cause);
        } catch (RuntimeException e) {
            throw new OutboxDeliveryException("Error publishing ORDER_SHIPPED event to Kafka. Order ID: " + event.orderId(), e);
        }
    }
}