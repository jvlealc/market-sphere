package io.github.jvlealc.marketsphere.orders.service;

import io.github.jvlealc.marketsphere.orders.client.banking.representation.BankingPaymentRepresentation;
import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.dto.*;
import io.github.jvlealc.marketsphere.orders.dto.*;
import io.github.jvlealc.marketsphere.orders.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.exception.client.products.ProductClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.facade.OrderDependenciesFacade;
import io.github.jvlealc.marketsphere.orders.mapper.OrderDetailsMapper;
import io.github.jvlealc.marketsphere.orders.mapper.OrderItemDetailsMapper;
import io.github.jvlealc.marketsphere.orders.mapper.OrderMapper;
import io.github.jvlealc.marketsphere.orders.model.Order;
import io.github.jvlealc.marketsphere.orders.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.model.PaymentInfo;
import io.github.jvlealc.marketsphere.orders.model.enums.OrderStatus;
import io.github.jvlealc.marketsphere.orders.model.enums.PaymentType;
import io.github.jvlealc.marketsphere.orders.repository.OrderItemRepository;
import io.github.jvlealc.marketsphere.orders.repository.OrderRepository;
import io.github.jvlealc.marketsphere.orders.validator.OrderValidator;
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
public class OrderServiceImpl implements OrderService {

    private static final String PENDING_PAYMENT_OBSERVATION_MESSAGE = "Created order. Awaiting payment.";
    private static final String INITIATE_PAYMENT_OBSERVATION_MESSAGE = "New payment made. Awaiting processing.";

    // Repositories e Validator do domínio
    private final OrderRepository repository;
    private final OrderItemRepository orderItemRepository;
    private final OrderValidator validator;
    // Facade
    private final OrderDependenciesFacade orderDependenciesFacade;
    // Mappers
    private final OrderMapper mapper;
    private final OrderDetailsMapper orderDetailsMapper;
    private final OrderItemDetailsMapper orderItemDetailsMapper;

    @Transactional
    @Override
    public OrderResponseDto createOrder(OrderRequestDto orderRequestDto) {
        List<Long> productIds = this.extractProductsIds(orderRequestDto);
        Map<Long, ProductRepresentation> productsMap = orderDependenciesFacade.getProductsByIds(productIds);
        CustomerRepresentation customerRepresentation = orderDependenciesFacade.getCustomerById(orderRequestDto.customerId());
        validator.validate(productIds, productsMap, customerRepresentation);
        Order createdOrder = this.performPersistence(orderRequestDto, productsMap);
        return mapper.toOrderDto(createdOrder);
    }

    @Transactional
    @Override
    public void initiatePayment(Long orderId, String metadata, PaymentType paymentType) {
        Order existingOrder = this.findOrderById(orderId);

        PaymentInfo paymentInfo = new PaymentInfo();
        paymentInfo.setPaymentType(paymentType);
        paymentInfo.setMetadata(metadata);
        existingOrder.setStatus(OrderStatus.PLACED);
        existingOrder.setObservations(INITIATE_PAYMENT_OBSERVATION_MESSAGE);

        // Atualizar o pedido com a chave de pagamento emitida pelo banco (simulação)
        BankingPaymentRepresentation bankingPaymentRepresentation = orderDependenciesFacade.requestPayment(existingOrder.getId());
        existingOrder.setPaymentKey(bankingPaymentRepresentation.paymentKey());
    }

    @Transactional(readOnly = true)
    @Override
    public OrderResponseDto getOrderById(Long orderId) {
        return mapper.toOrderDto( this.findOrderById(orderId) );
    }

    @Transactional(readOnly = true)
    @Override
    public OrderDetailsResponseDto getOrderDetailsById(Long orderId) {
        Order existingOrder = this.findOrderById(orderId);
        CustomerRepresentation existingCustomer = orderDependenciesFacade.getCustomerByIdIgnoringFilter(existingOrder.getCustomerId());
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

        Map<Long, ProductRepresentation> productRepresentationMap = orderDependenciesFacade.getProductsByIds(productsIds);

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
