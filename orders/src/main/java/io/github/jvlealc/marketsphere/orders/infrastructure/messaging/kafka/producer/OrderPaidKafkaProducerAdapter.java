package io.github.jvlealc.marketsphere.orders.infrastructure.messaging.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidEvent;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.MessagingPublishException;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.MessagingSerializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

@Component
public class OrderPaidKafkaProducerAdapter implements OrderPaidPublisherPort {

    private final ObjectMapper objectMapper;
    private final KafkaTemplate<String, String> kafkaTemplate;

    private final String topic;

    public OrderPaidKafkaProducerAdapter(
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${market-sphere.kafka.config.topics.paid-orders}") String topic
    ) {
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Override
    public void publish(OrderPaidEvent event) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(event);
            String messageKey = event.orderId().toString();

            kafkaTemplate.send(topic, messageKey, jsonPayload).get();

        } catch (JsonProcessingException e) {
            throw new MessagingSerializationException("Error serializing ORDER_PAID event to Kafka. Order ID: " + event.orderId(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessagingPublishException("Thread interrupted while publishing ORDER_PAID event to Kafka. Order ID: " + event.orderId(), e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new MessagingPublishException("Error publishing ORDER_PAID event to Kafka. Order ID: " + event.orderId(), cause);
        } catch (RuntimeException e) {
            throw new MessagingPublishException("Error publishing ORDER_PAID event to Kafka. Order ID: " + event.orderId(), e);
        }
    }
}
