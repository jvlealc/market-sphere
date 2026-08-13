package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.OrderItemCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PaymentInfoCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PlaceOrderCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.model.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.model.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.service.OrderPlacementService;
import io.github.jvlealc.marketsphere.orders.application.service.ProductLookupService;
import io.github.jvlealc.marketsphere.orders.application.policy.CustomerEligibilityPolicy;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.CustomerSnapshot;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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

    public Long execute(PlaceOrderCommand command) {
        Objects.requireNonNull(command, "Place order command is required");

        CustomerProfile customer = customerGateway.getCustomerById(command.customerId());
        customerPolicy.ensureActive(customer);

        List<Long> productIds = command.orderItems().stream()
                .map(OrderItemCommand::productId)
                .distinct()
                .toList();
        Map<Long, ProductSnapshot> products = productLookupService.getAvailableProductsByIds(productIds);

        List<OrderItem> orderItems = mapToOrderItemDomains(command.orderItems(), products);
        PaymentInfo paymentInfo = mapToPaymentInfoDomain(command.paymentInfo());

        Order newOrder = Order.createNew(
                customer.customerId(),
                toCustomerSnapshot(customer),
                paymentInfo,
                orderItems
        );

        return orderPlacement.place(newOrder, EventLineage.start());
    }

    private static List<OrderItem> mapToOrderItemDomains(List<OrderItemCommand> commands, Map<Long, ProductSnapshot> products) {
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

    private static CustomerSnapshot toCustomerSnapshot(CustomerProfile customer) {
        return new CustomerSnapshot(
                customer.fullName(),
                customer.nationalId(),
                customer.email(),
                customer.phoneNumber(),
                customer.postalCode(),
                customer.street(),
                customer.houseNumber(),
                customer.complement(),
                customer.neighborhood(),
                customer.city(),
                customer.state(),
                customer.country()
        );
    }

    private static PaymentInfo mapToPaymentInfoDomain(PaymentInfoCommand command) {
        return PaymentInfo.createNew(command.metadata(), command.paymentType());
    }
}
