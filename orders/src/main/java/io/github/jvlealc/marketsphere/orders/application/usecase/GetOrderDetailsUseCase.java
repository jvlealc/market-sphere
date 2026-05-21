package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.ProductNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidQueryException;
import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderItemDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderDetailsByIdQuery;
import io.github.jvlealc.marketsphere.orders.application.service.ProductLookupService;
import io.github.jvlealc.marketsphere.orders.application.support.ProductIdsExtractor;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public final class GetOrderDetailsUseCase {

    private final OrderRepositoryPort orderRepository;
    private final CustomerGatewayPort customerGateway;
    private final ProductLookupService productLookupService;


    public OrderDetailsOutput execute(GetOrderDetailsByIdQuery query) {
        if (query == null || query.orderId() == null) {
            throw new InvalidQueryException("Order ID is required");
        }

        Order order = orderRepository.findWithDetailsById(query.orderId())
                .orElseThrow(() -> new OrderNotFoundException(query.orderId()));

        CustomerProfile customer = customerGateway.getCustomerByIdIncludingInactive(order.getCustomerId());

        List<Long> productIds = ProductIdsExtractor.extract(order.getOrderItems(), OrderItem::getProductId);
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
                order.getInvoiceUrl(),
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
