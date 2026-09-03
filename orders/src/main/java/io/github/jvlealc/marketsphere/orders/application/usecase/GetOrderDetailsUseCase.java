package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderItemDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.model.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderDetailsByIdQuery;
import io.github.jvlealc.marketsphere.orders.application.service.ProductLookupService;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public final class GetOrderDetailsUseCase {

    private final OrderRepositoryPort orderRepository;
    private final CustomerGatewayPort customerGateway;
    private final ProductLookupService productLookupService;


    public OrderDetailsOutput execute(GetOrderDetailsByIdQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        Order order = orderRepository.findWithDetailsById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        CustomerProfile customer = customerGateway.getCustomerByIdIncludingInactive(order.getCustomerId());

        List<Long> productIds = order.getOrderItems().stream()
                .map(OrderItem::getProductId)
                .distinct()
                .toList();
        Map<Long, ProductSnapshot> products = productLookupService.getProductsByIdsIncludingInactive(productIds);

        return toOutput(order, products, customer);
    }

    private static OrderDetailsOutput toOutput(Order order, Map<Long, ProductSnapshot> products, CustomerProfile customer) {
        List<OrderItemDetailsOutput> orderItems = order.getOrderItems()
                .stream()
                .map(item -> toOutput(item, getProductOrThrow(item.getProductId(), products)))
                .toList();

        return new OrderDetailsOutput(
                order.getId(),
                customer,
                order.getOrderDate(),
                order.getPaidAt(),
                order.getBilledAt(),
                order.getShippedAt(),
                order.getTotal(),
                order.getStatus(),
                order.getObservations(),
                order.getInvoiceId(),
                order.getTrackingCode(),
                orderItems
        );
    }

    private static OrderItemDetailsOutput toOutput(OrderItem orderItem, ProductSnapshot product) {
        return new OrderItemDetailsOutput(
                orderItem.getProductId(),
                product.name(),
                orderItem.getAmount(),
                orderItem.getUnitPrice(),
                product.active()
        );
    }

    private static ProductSnapshot getProductOrThrow(Long productId, Map<Long, ProductSnapshot> products) {
        ProductSnapshot product = products.get(productId);
        if (product == null) {
            throw new ProductNotFoundException("productId", "Product not found for order item. Product ID: " + productId);
        }
        return product;
    }
}
