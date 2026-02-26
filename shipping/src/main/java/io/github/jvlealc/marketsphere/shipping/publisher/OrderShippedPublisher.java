package io.github.jvlealc.marketsphere.shipping.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingSerializationException;
import io.github.jvlealc.marketsphere.shipping.publisher.event.OrderShippedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Kafka Publisher responsável por serializar e publicar o evento
 * {@link OrderShippedEvent} no tópico de pedidos enviados.
 */
@Component
public class OrderShippedPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderShippedPublisher.class);

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${market-sphere.config.kafka.topics.shipped-orders}")
    private String topic;

    public OrderShippedPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    // Realiza a publicação do evento de envio do pedido no tópico do Kafka
    public void publish(OrderShippedEvent orderShippedEvent) {
        log.info("Publishing shipped order with ID: {}", orderShippedEvent.orderId());
        try {
            // Serializa o evento
            String jsonPayload = objectMapper.writeValueAsString(orderShippedEvent);
            //Usa o ID do pedido como chave da mensagem
            String orderIdKey = String.valueOf(orderShippedEvent.orderId());

            // Publica mensagem no tópico do Kafka
            kafkaTemplate.send(this.topic, orderIdKey, jsonPayload);

            log.info("Published event for order ID: {} and tracking code: {}",
                    orderShippedEvent.orderId(), orderShippedEvent.trackingCode()
            );
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for paid-orders topic. Order ID: {}", orderShippedEvent.orderId(), e);
            throw new MessagingSerializationException("Error serializing message for Kafka", e);
        } catch (RuntimeException e) {
            log.error("Error sending message to shipped-orders topic. Order ID: {}", orderShippedEvent.orderId(), e);
            throw e;
        }
    }
}
