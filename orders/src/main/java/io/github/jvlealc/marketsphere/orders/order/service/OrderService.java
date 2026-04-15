package io.github.jvlealc.marketsphere.orders.order.service;

import io.github.jvlealc.marketsphere.orders.client.banking.BankingPaymentRepresentation;
import io.github.jvlealc.marketsphere.orders.client.customers.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.order.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.client.products.ProductClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.order.facade.OrderIntegrationFacade;
import io.github.jvlealc.marketsphere.orders.order.dto.*;
import io.github.jvlealc.marketsphere.orders.order.mapper.OrderDetailsMapper;
import io.github.jvlealc.marketsphere.orders.order.mapper.OrderItemDetailsMapper;
import io.github.jvlealc.marketsphere.orders.order.mapper.OrderMapper;
import io.github.jvlealc.marketsphere.orders.order.model.Order;
import io.github.jvlealc.marketsphere.orders.order.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.order.model.PaymentInfo;
import io.github.jvlealc.marketsphere.orders.order.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.order.model.enums.PaymentType;
import io.github.jvlealc.marketsphere.orders.order.repository.OrderItemRepository;
import io.github.jvlealc.marketsphere.orders.order.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.order.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private static final String PENDING_PAYMENT_OBSERVATION_MESSAGE = "Created order. Awaiting payment.";
    private static final String INITIATE_PAYMENT_OBSERVATION_MESSAGE = "New payment made. Awaiting processing.";

    // Repositories e Validator do domínio
    private final OrderRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final OrderValidator validator;
    // Facade
    private final OrderIntegrationFacade orderIntegrationFacade;
    // Mappers
    private final OrderMapper mapper;
    private final OrderDetailsMapper orderDetailsMapper;
    private final OrderItemDetailsMapper orderItemDetailsMapper;

    @Transactional
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto) {
        List<Long> productIds = this.extractProductsIds(orderRequestDto);
        Map<Long, ProductRepresentation> productsMap = orderIntegrationFacade.getProductsByIds(productIds);
        CustomerRepresentation customerRepresentation = orderIntegrationFacade.getCustomerById(orderRequestDto.customerId());
        validator.validate(productIds, productsMap, customerRepresentation);
        Order createdOrder = this.performPersistence(orderRequestDto, productsMap);
        return mapper.toOrderDto(createdOrder);
    }

    @Transactional
    public void initiatePayment(Long orderId, String metadata, PaymentType paymentType) {
        Order existingOrder = this.findOrderById(orderId);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setMetadata(metadata);
        existingOrder.setStatus(OrderStatus.PLACED);
        existingOrder.setObservations(INITIATE_PAYMENT_OBSERVATION_MESSAGE);

        // Atualizar o pedido com a chave de pagamento emitida pelo banco (simulação)
        BankingPaymentRepresentation bankingPaymentRepresentation = orderIntegrationFacade.requestPayment(existingOrder.getId());
        existingOrder.setPaymentKey(bankingPaymentRepresentation.paymentKey());
    }

    @Transactional(readOnly = true)
    public OrderResponseDto getOrderById(Long orderId) {
        return mapper.toOrderDto( this.findOrderById(orderId) );
    }

    @Transactional(readOnly = true)
    public OrderDetailsResponseDto getOrderDetailsById(Long orderId) {
        Order existingOrder = this.findOrderById(orderId);
        CustomerRepresentation existingCustomer = orderIntegrationFacade.getCustomerByIdIgnoringFilter(existingOrder.getCustomerId());
        List<OrderItemDetailsResponseDto> orderItemDtos = this.getOrderItemDetailsDtos(existingOrder);
        return orderDetailsMapper.toOrderDetailsDto(existingOrder, existingCustomer, orderItemDtos);
    }

    // ---- MÉTODOS PRIVADOS ---- //
    private Order findOrderById(Long orderId) {
        return repository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    private List<Long> extractProductsIds(OrderRequestDto orderRequestDto) {
        return orderRequestDto.orderItems()
                .stream()
                .map(OrderItemRequestDto::productId)
                .toList();
    }

    private Order performPersistence(OrderRequestDto orderRequestDto, Map<Long, ProductRepresentation> productsMap) {
        Order orderToPersist = mapper.toOrderEntity(orderRequestDto, productsMap);
        orderToPersist.setStatus(OrderStatus.PENDING);
        orderToPersist.setObservations(PENDING_PAYMENT_OBSERVATION_MESSAGE);
        Order persistedOrder = repository.save(orderToPersist);
        if (persistedOrder.getOrderItems() != null) {
            orderItemRepository.saveAll(persistedOrder.getOrderItems());
        }
        return persistedOrder;
    }

    // Helper para a API REST (Leitura de detalhada)
    private List<OrderItemDetailsResponseDto> getOrderItemDetailsDtos(Order order) {
        if (order.getOrderItems() == null || order.getOrderItems().isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> productsIds = order.getOrderItems()
                .stream()
                .map(OrderItem::getProductId)
                .toList();

        Map<Long, ProductRepresentation> productRepresentationMap = orderIntegrationFacade.getProductsByIds(productsIds);

        return order.getOrderItems()
                .stream()
                .map(orderItem -> {
                    ProductRepresentation productRepresentation = productRepresentationMap.get(orderItem.getProductId());
                    if (productRepresentation == null) {
                        log.error(
                                "Data integrity failure: Product ID {} (from Order ID: {}) not found in 'products' service during details request.",
                                orderItem.getProductId(), order.getId()
                        );
                        throw new ProductClientNotFoundException("productId", "Product with ID " + orderItem.getProductId() + " not found (orphaned data).");
                    }
                    return orderItemDetailsMapper.toOrderItemDetailsDto(orderItem, productRepresentation);
                })
                .toList();
    }
}
