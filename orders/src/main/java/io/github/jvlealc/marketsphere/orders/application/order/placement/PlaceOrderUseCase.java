package io.github.jvlealc.marketsphere.orders.application.order.placement;

import io.github.jvlealc.marketsphere.orders.application.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.customer.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.customer.CustomerAddress;
import io.github.jvlealc.marketsphere.orders.application.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.product.ProductLookupService;
import io.github.jvlealc.marketsphere.orders.application.customer.CustomerEligibilityPolicy;
import io.github.jvlealc.marketsphere.orders.domain.order.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final ProductLookupService productLookupService;
    private final CustomerGatewayPort customerGateway;
    private final CustomerEligibilityPolicy customerPolicy;
    private final OrderPlacementService orderPlacement;
    private final Clock clock;

    public Long execute(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        CustomerProfile customer = customerGateway.getCustomerById(command.customerId());
        customerPolicy.ensureActive(customer);
        customerPolicy.ensureHasAddress(customer);

        List<Long> productIds = command.orderItems().stream()
                .map(OrderItemCommand::productId)
                .distinct()
                .toList();

        if (productIds.size() > Order.MAX_DISTINCT_PRODUCTS) {
            throw new InvalidOrderException("Product count exceeds maximum number of products");
        }

        Map<Long, ProductSnapshot> products = productLookupService.getAvailableProductsByIds(productIds);

        Instant now = Instant.now(clock);

        List<OrderItem> orderItems = OrderItem.consolidateByProduct(
                toOrderItemDomains(command.orderItems(), products)
        );
        PaymentInfo paymentInfo = toPaymentInfoDomain(command.paymentInfo(), now);

        Order newOrder = Order.createNew(
                customer.customerId(),
                toCustomerSnapshot(customer),
                paymentInfo,
                orderItems,
                now
        );

        return orderPlacement.place(newOrder, EventLineage.start());
    }

    private static List<OrderItem> toOrderItemDomains(List<OrderItemCommand> commands, Map<Long, ProductSnapshot> products) {
        return commands.stream()
                .map(item -> {
                    ProductSnapshot product = products.get(item.productId());
                    if (product == null) {
                        throw new InvalidCommandException("Product not found for order item. Product ID: " + item.productId());
                    }

                    return OrderItem.createNew(item.productId(), product.name(), item.amount(), product.unitPrice());
                })
                .toList();
    }

    private static PaymentInfo toPaymentInfoDomain(PaymentInfoCommand command, Instant createdAt) {
        return PaymentInfo.createNew(command.metadata(), command.paymentType(), createdAt);
    }

    private static CustomerSnapshot toCustomerSnapshot(CustomerProfile customer) {
        CustomerAddress address = customer.address();

        return new CustomerSnapshot(
                customer.fullName(),
                customer.nationalId(),
                customer.email(),
                customer.phoneNumber(),
                address.postalCode(),
                address.street(),
                address.houseNumber(),
                address.complement(),
                address.neighborhood(),
                address.city(),
                address.state(),
                address.country()
        );
    }
}
