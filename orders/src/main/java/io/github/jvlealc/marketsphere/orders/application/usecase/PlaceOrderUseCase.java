package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.OrderItemCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PaymentInfoCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PlaceOrderCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.*;
import io.github.jvlealc.marketsphere.orders.application.ports.out.product.ProductSnapshot;
import io.github.jvlealc.marketsphere.orders.application.service.ProductLookupService;
import io.github.jvlealc.marketsphere.orders.application.support.ProductIdsExtractor;
import io.github.jvlealc.marketsphere.orders.application.validator.CustomerEligibilityValidator;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import io.github.jvlealc.marketsphere.orders.domain.model.OrderItem;
import io.github.jvlealc.marketsphere.orders.domain.model.vo.PaymentInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final ProductLookupService productLookupService;
    private final CustomerGatewayPort customerGateway;
    private final CustomerEligibilityValidator customerValidator;

    @Transactional
    public Long execute(PlaceOrderCommand command) {
        validateCommandConsistency(command);

        CustomerProfile customer = customerGateway.getCustomerById(command.customerId());
        customerValidator.validateActive(customer);

        List<Long> productIds = ProductIdsExtractor.extract(command.orderItems(), OrderItemCommand::productId);
        Map<Long, ProductSnapshot> products = productLookupService.getAvailableProductsByIds(productIds);

        List<OrderItem> orderItems = mapToOrderItemDomains(command.orderItems(), products);
        PaymentInfo paymentInfo = mapToPaymentInfoDomain(command.paymentInfo());

        Order newOrder = Order.createNew(
                customer.customerId(),
                paymentInfo,
                orderItems
        );

        Order savedOrder = orderRepository.save(newOrder);

        OutboxMessage paymentRequestMessage = createPaymentRequestOutboxMessage(savedOrder);
        outboxRepository.save(paymentRequestMessage);

        return savedOrder.getId();
    }

    private static void validateCommandConsistency(PlaceOrderCommand command) {
        if (command == null) {
            throw new InvalidCommandException("Place order command is required");
        }

        if (command.customerId() == null) {
            throw new InvalidCommandException("Customer ID is required");
        }

        if (command.paymentInfo() == null || command.paymentInfo().paymentType() == null) {
            throw new InvalidCommandException("Payment info with type is required");
        }

        if (command.orderItems() == null || command.orderItems().isEmpty()) {
            throw new InvalidCommandException("Order must contain at least one item");
        }

        validateOrderItemsConsistency(command.orderItems());
    }

    private static void validateOrderItemsConsistency(final List<OrderItemCommand> commands) {
        for (OrderItemCommand cmd : commands) {
            if (cmd == null) {
                throw new InvalidCommandException("Order items must not contain null values");
            }

            if (cmd.productId() == null) {
                throw new InvalidCommandException("Product ID is required");
            }

            if (cmd.amount() == null ||  cmd.amount() <= 0) {
                throw new InvalidCommandException("Amount must be greater than zero");
            }
        }
    }

    private static List<OrderItem> mapToOrderItemDomains(List<OrderItemCommand> commands, Map<Long, ProductSnapshot> products) {
        return commands.stream()
                .map(item -> {
                    ProductSnapshot product = products.get(item.productId());
                    if (product == null) {
                        throw new InvalidCommandException("Product not found for order item. Product ID: " + item.productId());
                    }

                    return OrderItem.createNew(item.productId(), item.amount(), product.unitPrice());
                })
                .toList();
    }

    private static PaymentInfo mapToPaymentInfoDomain(PaymentInfoCommand command) {
        return PaymentInfo.createNew(command.metadata(), command.paymentType());
    }

    private static OutboxMessage createPaymentRequestOutboxMessage(Order order) {
        String payload = """
                {
                    "orderId": %d
                }
                """.formatted(order.getId());
        String idempotencyKey = "payment-request-order-" + order.getId();

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                order.getId().toString(),
                OutboxEventType.PAYMENT_REQUEST_REQUIRED,
                OutboxChannel.PAYMENT,
                payload,
                idempotencyKey
        );
    }
}
