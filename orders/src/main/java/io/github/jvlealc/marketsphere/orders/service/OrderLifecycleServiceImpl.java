package io.github.jvlealc.marketsphere.orders.service;

import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.exception.client.products.ProductClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.facade.OrderDependenciesFacade;
import io.github.jvlealc.marketsphere.orders.model.Order;
import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.publisher.OrderPaymentPublisher;
import io.github.jvlealc.marketsphere.orders.publisher.event.OrderItemPayload;
import io.github.jvlealc.marketsphere.orders.publisher.event.OrderPaidEvent;
import io.github.jvlealc.marketsphere.orders.publisher.mapper.OrderItemPayloadMapper;
import io.github.jvlealc.marketsphere.orders.publisher.mapper.OrderPaidEventMapper;
import io.github.jvlealc.marketsphere.orders.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.service.notification.EmailService;
import io.github.jvlealc.marketsphere.orders.subscriber.event.OrderBilledEvent;
import io.github.jvlealc.marketsphere.orders.subscriber.event.OrderShippedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private static final String PAID_OBSERVATION_MESSAGE = "Payment successfully confirmed. Awaiting billing.";
    private static final String BILLED_OBSERVATION_MESSAGE = "Order billed and invoice generated. Awaiting shipment.";
    private static final String SHIPPED_OBSERVATION_MESSAGE = "Order has been shipped. Tracking code is now available.";

    // Repository
    private final OrderRepository repository;
    // Facade
    private final OrderDependenciesFacade orderDependenciesFacade;
    // Mappers
    private final OrderPaidEventMapper orderPaidEventMapper;
    private final OrderItemPayloadMapper orderItemPayloadMapper;
    // Kafka Publisher
    private final OrderPaymentPublisher paymentPublisher;
    // Service
    private final EmailService emailService;

    @Transactional
    @Override
    public void processSuccessfulPayment(Long orderId) {
        Order existingOrder = this.findOrderById(orderId);

        existingOrder.setStatus(OrderStatus.PAID);
        existingOrder.setPaidAt(Instant.now());
        existingOrder.setObservations(PAID_OBSERVATION_MESSAGE);

        CustomerRepresentation existingCustomer = orderDependenciesFacade.getCustomerById(existingOrder.getCustomerId());
        List<OrderItemPayload> orderItemRepresentations = this.getOrderItemPayload(existingOrder);
        OrderPaidEvent orderPaidEvent = orderPaidEventMapper.toOrderEvent(existingOrder, existingCustomer, orderItemRepresentations);

        paymentPublisher.publish(orderPaidEvent);
    }

    @Transactional
    @Override
    public void processPaymentError(Order order, String observations) {
        log.warn("Processing payment failure for the order: {}", order.getId());
        order.setStatus(OrderStatus.PAYMENT_ERROR);
        order.setObservations(observations);
    }

    @Transactional
    @Override
    public void processOrderBilled(OrderBilledEvent orderBilledEvent) {
        Order existingOrder = this.findOrderById(orderBilledEvent.orderId());
        existingOrder.setStatus(OrderStatus.BILLED);
        existingOrder.setBilledAt(orderBilledEvent.billedAt());
        existingOrder.setInvoiceUrl(orderBilledEvent.invoiceUrl());
        existingOrder.setObservations(BILLED_OBSERVATION_MESSAGE);
    }

    @Transactional
    @Override
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
            CustomerRepresentation customer = orderDependenciesFacade.getCustomerById(existingOrder.getCustomerId());

            // Chama o méto-do assíncrono de envio de email
            emailService.sendShipmentConfirmationEmail(customer, existingOrder);
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

        Map<Long, ProductRepresentation> productRepresentations = orderDependenciesFacade.getProductsByIds(productsIds);

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
