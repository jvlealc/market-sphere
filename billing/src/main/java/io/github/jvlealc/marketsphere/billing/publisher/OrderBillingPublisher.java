package io.github.jvlealc.marketsphere.billing.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.billing.exception.MessagingSerializationException;
import io.github.jvlealc.marketsphere.billing.model.Order;
import io.github.jvlealc.marketsphere.billing.publisher.event.OrderBilledEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class OrderBillingPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // Nome do tópico Kafka usado para publicar pedidos faturados
    @Value("${market-sphere.config.kafka.topics.billed-orders}")
    private String topic;

    public OrderBillingPublisher(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }
    /**
     * Publica um evento de pedido faturado no Kafka.
     *
     * @param order pedido a ser publicado
     * @param invoiceUrl URL do comprovante de fatura
     * @throws MessagingSerializationException se ocorrer erro na serialização JSON
     */
    public void publish(Order order, String invoiceUrl) {
        try {
            // Cria o evento com o identificador do pedido, URL da fatura e instante de faturamento
            OrderBilledEvent orderBilledEvent = new OrderBilledEvent(
                    order.orderId(),
                    invoiceUrl,
                    Instant.now()
            );
            // Converte o evento para JSON
            String jsonPayload = objectMapper.writeValueAsString(orderBilledEvent);

            // Define a chave da mensagem como o ID do pedido
            String orderIdKey = String.valueOf(orderBilledEvent.orderId());

            // Envia a mensagem para o tópico Kafka
            kafkaTemplate.send(topic, orderIdKey, jsonPayload);

        } catch (JsonProcessingException e) {
            log.error("Error serializing message for billed-orders topic. Order ID: {}", order.orderId(), e);
            throw new MessagingSerializationException("Error serializing message for Kafka", e);
        } catch (RuntimeException e) {
            log.error("Error sending message to paid-orders topic. Order ID: {}", order.orderId(), e);
            throw e;
        }
    }
}
