package io.github.jvlealc.marketsphere.orders.messaging.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.messaging.MessagingSerializationException;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.event.OrderPaidEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderPaymentPublisher {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${market-sphere.config.kafka.topics.paid-orders}")
    private String topic;

    // Realiza a publicação do evento de pagamento no tópico do Kafka
    public void publish(OrderPaidEvent orderPaidEvent) {
        log.info("publishing paid order with ID: {}", orderPaidEvent.orderId());
        try {
            String jsonPayload = objectMapper.writeValueAsString(orderPaidEvent);
            String orderIdKey = String.valueOf(orderPaidEvent.orderId());
            kafkaTemplate.send(topic, orderIdKey, jsonPayload);
        } catch (JsonProcessingException e) {
            log.error("Error serializing message for paid-orders topic. Order ID: {}", orderPaidEvent.orderId(), e);
            throw new MessagingSerializationException("Error serializing message for Kafka", e);
        } catch (RuntimeException e) {
            log.error("Error sending message to paid-orders topic. Order ID: {}", orderPaidEvent.orderId(), e);
            throw e;
        }
    }
}
