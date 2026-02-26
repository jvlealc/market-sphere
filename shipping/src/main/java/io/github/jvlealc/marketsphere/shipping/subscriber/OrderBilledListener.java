package io.github.jvlealc.marketsphere.shipping.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.shipping.exception.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.shipping.exception.ProcessShipmentException;
import io.github.jvlealc.marketsphere.shipping.service.ShippingService;
import io.github.jvlealc.marketsphere.shipping.subscriber.event.OrderBilledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka Subscriber que ouve o tópico de pedidos faturados.
 * Responsável por desserializar a mensagem e delegar o processamento
 * para o {@link ShippingService}.
 */
@Component
public class OrderBilledListener {

    private static final Logger log = LoggerFactory.getLogger(OrderBilledListener.class);

    private final ObjectMapper objectMapper;
    private final ShippingService service;

    public OrderBilledListener(ObjectMapper objectMapper, ShippingService service) {
        this.objectMapper = objectMapper;
        this.service = service;
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.config.kafka.topics.billed-orders}"
    )
    public void listen(String jsonMessage) {
        log.info("Receive billed order message. Starting shipment processing");

        // Desserialização
        OrderBilledEvent orderBilledEvent;
        try {
            orderBilledEvent = objectMapper.readValue(jsonMessage, OrderBilledEvent.class);
            log.info("Message deserialized successfully for order ID: {}", orderBilledEvent.orderId());
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message. Discarding, Message: {}. Error: {}", jsonMessage, e.getMessage());
            throw new MessagingDeserializationException("Failed to parse OrderBilledEvent", e);
        }

        // Lógica de negócio - delega ao service o processamento de envio
        try {
            service.processShipment(orderBilledEvent);
            log.info("Shipment processed successfully for order ID: {}", orderBilledEvent.orderId());
        } catch (Exception e) {
            log.error("Failed to process shipment for order ID {}. Error: {}", orderBilledEvent.orderId(), e.getMessage(), e);
            throw new ProcessShipmentException("Shipment processing failed for order ID " + orderBilledEvent.orderId(), e);
        }
    }
}
