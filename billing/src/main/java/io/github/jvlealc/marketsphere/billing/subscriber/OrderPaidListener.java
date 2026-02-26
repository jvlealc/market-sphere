package io.github.jvlealc.marketsphere.billing.subscriber;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jvlealc.marketsphere.billing.exception.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.billing.exception.OrderProcessingException;
import io.github.jvlealc.marketsphere.billing.service.InvoiceGeneratorService;
import io.github.jvlealc.marketsphere.billing.subscriber.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class OrderPaidListener {

    private final ObjectMapper objectMapper;
    private final InvoiceGeneratorService invoiceService;


    public OrderPaidListener(ObjectMapper objectMapper, InvoiceGeneratorService invoiceService) {
        this.objectMapper = objectMapper;
        this.invoiceService = invoiceService;
    }

    @KafkaListener(
            groupId = "${spring.kafka.consumer.group-id}",
            topics = "${market-sphere.config.kafka.topics.paid-orders}"
    )
    public void listen(String jsonMessage) {
        log.info("Received OrderPaidEvent message. Starting billing processing...");

        // Desserialização
        OrderPaidEvent orderPaidEvent;
        try {
            // Converter o JSON enviado pelo producer para PaidOrderEvent
            orderPaidEvent = objectMapper.readValue(jsonMessage, OrderPaidEvent.class);
            log.debug("[OrderPaidEvent] Deserialized successfully for order ID: {}", orderPaidEvent.orderId());

        } catch (JsonProcessingException e) {
            log.error("[OrderPaidEvent] Failed to deserialize JSON. Message might be a poison pill. Error: {}", e.getMessage(), e);
            log.debug("[OrderPaidEvent] Corrupted JSON payload: {}", jsonMessage);
            throw new MessagingDeserializationException("Failed to deserialize OrderPaidEvent Kafka message", e);
        }

        // Lógica de negócio - delega ao service de faturamento
        try {
            invoiceService.generate(orderPaidEvent);
            log.info("Invoice generated successfully for order ID: {}", orderPaidEvent.orderId());

        }  catch (Exception e) {
            log.error("Failed to process order for billing for Order ID: {}. Reason: {}",
                    orderPaidEvent.orderId(), e.getMessage(), e);

            // Se for uma exceção de processamento esperada, apenas a relança
            if (e instanceof OrderProcessingException){
                throw e;
            }
            // Se inesperado, o encapsula
            throw new OrderProcessingException("Failed to generate invoice for order ID " + orderPaidEvent.orderId(), e);
        }
    }
}
