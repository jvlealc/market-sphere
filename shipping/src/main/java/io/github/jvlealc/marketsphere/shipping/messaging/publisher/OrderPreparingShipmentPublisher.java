package io.github.jvlealc.marketsphere.shipping.messaging.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingPublishException;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingSerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Kafka Publisher responsável por serializar e publicar o evento
 * {@link OrderPreparingShipmentEvent} no tópico de pedidos em preparação para envio.
 */
@Component
public class OrderPreparingShipmentPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderPreparingShipmentPublisher.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OrderPreparingShipmentPublisher(
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${market-sphere.kafka.config.topics.preparing-shipment-orders}") String topic
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publish(OrderPreparingShipmentEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            String messageKey = String.valueOf(event.orderId());

            kafkaTemplate.send(topic, messageKey, jsonPayload).get();

            log.info(
                    "ORDER_PREPARING_SHIPMENT event published successfully. Order ID: {}",
                    event.orderId()
            );

        } catch (JsonProcessingException e) {
            throw new MessagingSerializationException("Error serializing ORDER_PREPARING_SHIPMENT event to Kafka. Order ID: " + event.orderId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingPublishException("Thread interrupted while publishing ORDER_PREPARING_SHIPMENT event to Kafka. Order ID: " + event.orderId(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new MessagingPublishException("Error publishing ORDER_PREPARING_SHIPMENT event to Kafka. Order ID: " + event.orderId(), cause);
        } catch (RuntimeException e) {
            throw new MessagingPublishException("Error publishing ORDER_PREPARING_SHIPMENT event to Kafka. Order ID: " + event.orderId(), e);
        }
    }
}