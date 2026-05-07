package io.github.jvlealc.marketsphere.orders.order.service;

import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.order.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.client.products.ProductClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.order.facade.OrderIntegrationFacade;
import io.github.jvlealc.marketsphere.orders.order.model.Order;
import io.github.jvlealc.marketsphere.orders.order.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.OrderPaymentPublisher;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.event.OrderItemPayload;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.event.OrderPaidEvent;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.mapper.OrderItemPayloadMapper;
import io.github.jvlealc.marketsphere.orders.messaging.publisher.mapper.OrderPaidEventMapper;
import io.github.jvlealc.marketsphere.orders.order.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.notification.NotificationService;
import io.github.jvlealc.marketsphere.orders.messaging.subscriber.event.OrderBilledEvent;
import io.github.jvlealc.marketsphere.orders.messaging.subscriber.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Serviço responsável por gerenciar o ciclo de vida de um Pedido.
 *
 * Esta interface lida especificamente com as transições de estado que
 * são iniciadas por eventos assíncronos como mensagens do Kafka,
 * notificações de Webhook.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleService {

    private static final String PAID_OBSERVATION_MESSAGE = "Payment successfully confirmed. Awaiting billing.";
    private static final String BILLED_OBSERVATION_MESSAGE = "Order billed and invoice generated. Awaiting shipment.";
    private static final String SHIPPED_OBSERVATION_MESSAGE = "Order has been shipped. Tracking code is now available.";

    // Repository
    private final OrderRepository repository;
    // Facade
    private final OrderIntegrationFacade orderIntegrationFacade;
    // Mappers
    private final OrderPaidEventMapper orderPaidEventMapper;
    private final OrderItemPayloadMapper orderItemPayloadMapper;
    // Kafka Publisher
    private final OrderPaymentPublisher paymentPublisher;
    // Service
    private final NotificationService notificationService;

    /**
     * Processa um evento de pagamento bem-sucedido.
     * Altera o status do pedido para PAID e publica o próximo evento.
     *
     * @param orderId O ID do pedido que foi pago.
     */
    @Transactional
    public void processSuccessfulPayment(Long orderId) {
        Order existingOrder = this.findOrderById(orderId);

        existingOrder.setStatus(OrderStatus.PAID);
        existingOrder.setPaidAt(Instant.now());
        existingOrder.setObservations(PAID_OBSERVATION_MESSAGE);

        CustomerRepresentation existingCustomer = orderIntegrationFacade.getCustomerById(existingOrder.getCustomerId());
        List<OrderItemPayload> orderItemRepresentations = this.getOrderItemPayload(existingOrder);
        OrderPaidEvent orderPaidEvent = orderPaidEventMapper.toOrderEvent(existingOrder, existingCustomer, orderItemRepresentations);

        paymentPublisher.publish(orderPaidEvent);
    }

    /**
     * Processa um evento de falha no pagamento.
     * Altera o status do pedido para PAYMENT_ERROR.
     *
     * @param order O pedido que falhou.
     * @param observations Detalhes da falha.
     */
    @Transactional
    public void processPaymentError(Order order, String observations) {
        log.warn("Processing payment failure for the order: {}", order.getId());
        order.setStatus(OrderStatus.PAYMENT_ERROR);
        order.setObservations(observations);
    }

    /**
     * Processa um evento de pedido faturado vindo do Kafka.
     * Altera o status do pedido para BILLED.
     *
     * @param orderBilledEvent O evento contendo os dados do faturamento.
     */
    @Transactional
    public void processOrderBilled(OrderBilledEvent orderBilledEvent) {
        Order existingOrder = this.findOrderById(orderBilledEvent.orderId());
        existingOrder.setStatus(OrderStatus.BILLED);
        existingOrder.setBilledAt(orderBilledEvent.billedAt());
        existingOrder.setInvoiceUrl(orderBilledEvent.invoiceUrl());
        existingOrder.setObservations(BILLED_OBSERVATION_MESSAGE);
    }

    /**
     * Processa um evento de pedido enviado vindo do Kafka.
     * Altera o status do pedido para SHIPPED.
     *
     * @param orderShippedEvent O evento contendo os dados do envio.
     */
    @Transactional
    public void processOrderShipped(OrderShippedEvent orderShippedEvent) {

        // Lógica CRITICA - Transacional
        Order existingOrder = this.findOrderById(orderShippedEvent.orderId());
        existingOrder.setStatus(OrderStatus.SHIPPED);
        existingOrder.setShippedAt(orderShippedEvent.shippedAt());
        existingOrder.setTrackingCode(orderShippedEvent.trackingCode());
        existingOrder.setObservations(SHIPPED_OBSERVATION_MESSAGE);

        // Lógica NÃO-CRITICA - Servico de notificação
        try {
            // buscar dados do cliente para o email
            CustomerRepresentation customer = orderIntegrationFacade.getCustomerById(existingOrder.getCustomerId());

            // Chama o méto-do assíncrono de envio de email
            notificationService.sendShipmentConfirmation(customer, existingOrder);
        } catch (Exception e) {
            // Apenas loga o erro e NÃO relança a exceção.
            // A transação CRITICA não será afetada.
            log.error("Failed to send shipment confirmation email for order ID: {}. Error: {}",
                    existingOrder.getId(), e.getMessage());
        }
    }

    // ---- MÉTODOS PRIVADOS (helpers) ---- //

    private Order findOrderById(Long orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    // Helper para o KAFKA (Evento)
    private List<OrderItemPayload> getOrderItemPayload(Order order) {
        List<Long> productsIds = order.getOrderItems()
                .stream()
                .map(OrderItem::getProductId)
                .toList();

        if (productsIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, ProductRepresentation> productRepresentations = orderIntegrationFacade.getProductsByIds(productsIds);

        return order.getOrderItems()
                .stream()
                .map(orderItem -> {
                    ProductRepresentation productRepresentation = productRepresentations.get(orderItem.getProductId());

                    if (productRepresentation == null) {
                        log.error(
                                "Data integrity failure: Product ID {} (from Order ID: {}) not found in 'products' service during event processing.",
                                orderItem.getProductId(), order.getId()
                        );

                        throw new ProductClientNotFoundException("productId", "Product with ID " + orderItem.getProductId() + " not found (orphaned data).");
                    }
                    return orderItemPayloadMapper.toOrderItemPayload(orderItem, productRepresentation);
                })
                .toList();
    }
}
