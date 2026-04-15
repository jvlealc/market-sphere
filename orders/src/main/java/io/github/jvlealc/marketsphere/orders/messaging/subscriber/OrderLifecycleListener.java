package io.github.jvlealc.marketsphere.orders.messaging.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.orders.messaging.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.orders.order.exception.OrderBilledProcessingException;
import io.github.jvlealc.marketsphere.orders.order.exception.OrderShippedProcessingException;
import io.github.jvlealc.marketsphere.orders.order.service.OrderLifecycleService;
import io.github.jvlealc.marketsphere.orders.messaging.subscriber.event.OrderBilledEvent;
import io.github.jvlealc.marketsphere.orders.messaging.subscriber.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka subscriber que escuta mensagens do evento {@link OrderBilledEvent}
 * e delega a atualização do status do pedido correspondente no sistema
 * ao serviço {@link OrderLifecycleService}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleListener {

    private final OrderLifecycleService orderLifecycleService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.config.kafka.topics.billed-orders}"
    )
    public void handleOrderBilled(String jsonMessage) {
        log.info("Received OrderBilledEvent message. Starting processing...");

        //Desserialização
        OrderBilledEvent orderBilledEvent;
        try {
            orderBilledEvent = objectMapper.readValue(jsonMessage, OrderBilledEvent.class);
            log.debug("[OrderBilledEvent] deserialized successfully for order ID: {}", orderBilledEvent.orderId());

        } catch (JsonProcessingException e) {
            log.error("[OrderBilledEvent] Failed to deserialize JSON. Message might be a poison pill. Error: {}", e.getMessage(), e);
            log.debug("[OrderBilledEvent] Corrupted JSON payload: {}", jsonMessage); // Loga o JSON apenas em DEBUG
            throw new MessagingDeserializationException("Failed to deserialize OrderBilledEvent Kafka message", e);
        }

        //  Lógica de Negócio - delega o processamento do pedido faturado ao service correto
        try {
            orderLifecycleService.processOrderBilled(orderBilledEvent);
            log.info("Order status successfully updated to BILLED for order ID: {}.", orderBilledEvent.orderId());

        } catch (Exception e) {
            log.error("Failed to process order billing for order ID: {}. Error: {}", orderBilledEvent.orderId(), e.getMessage(), e);
            throw new OrderBilledProcessingException("Failed to process order billing for order ID " + orderBilledEvent.orderId(), e);
        }
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.config.kafka.topics.shipped-orders}"
    )
    public void handleOrderShipped(String jsonMessage) {
        log.info("Received OrderShippedEvent message. Starting processing...");

        //  Desserialização
        OrderShippedEvent orderShippedEvent;
        try {
            orderShippedEvent = objectMapper.readValue(jsonMessage, OrderShippedEvent.class);
            log.debug("[OrderShippedEvent] Message deserialized successfully for order ID: {}", orderShippedEvent.orderId());

        } catch (JsonProcessingException e) {
            log.error("[OrderShippedEvent] Failed to deserialize JSON. Message might be a poison pill. Error: {}", e.getMessage(), e);
            log.debug("[OrderShippedEvent] Corrupted JSON payload: {}", jsonMessage);
            throw new MessagingDeserializationException("Failed to deserialize Kafka message", e);
        }

        //Lógica de Negócio - delega o processamento do pedido enviado ao service correto
        try {
            orderLifecycleService.processOrderShipped(orderShippedEvent);
            log.info("Order status successfully updated to SHIPPED for order ID: {}.", orderShippedEvent.orderId());

        } catch (Exception e) {
            log.error("Failed to process order shipping for order ID: {}. Error: {}", orderShippedEvent.orderId(), e.getMessage(), e);
            throw new OrderShippedProcessingException("Failed to process order shipping for order ID " + orderShippedEvent.orderId(), e);
        }
    }
}
