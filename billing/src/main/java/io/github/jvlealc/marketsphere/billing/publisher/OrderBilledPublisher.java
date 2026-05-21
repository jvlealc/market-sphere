package io.github.jvlealc.marketsphere.billing.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.billing.exception.MessagingSerializationException;
import io.github.jvlealc.marketsphere.billing.publisher.event.OrderBilledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderBilledPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Nome do tópico Kafka usado para publicar pedidos faturados
    @Value("${market-sphere.config.kafka.topics.billed-orders}")
    private String topic;

    public OrderBilledPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    /**
     * Publica um evento de pedido faturado no Kafka.
     *
     * @param event evento do pedido faturado
     * @throws MessagingSerializationException se ocorrer erro na serialização JSON
     */
    public void publish(OrderBilledEvent event) {
        try {
            // Converte o evento para JSON
            String jsonPayload = objectMapper.writeValueAsString(event);

            // Define a chave da mensagem como o ID do pedido
            String messageKey = String.valueOf(event.orderId());

            // Envia a mensagem para o tópico Kafka
            kafkaTemplate.send(topic, messageKey, jsonPayload);

        } catch (JsonProcessingException e) {
            log.error("Error serializing message for billed-orders topic. Order ID: {}", event.orderId(), e);
            throw new MessagingSerializationException("Error serializing message for Kafka", e);
        } catch (RuntimeException e) {
            log.error("Error sending message to billed-orders topic. Order ID: {}", event.orderId(), e);
            throw e;
        }
    }
}
