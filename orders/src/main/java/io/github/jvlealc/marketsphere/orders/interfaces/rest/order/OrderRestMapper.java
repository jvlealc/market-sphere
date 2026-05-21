package io.github.jvlealc.marketsphere.orders.interfaces.rest.order;

import io.github.jvlealc.marketsphere.orders.application.command.OrderItemCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PaymentInfoCommand;
import io.github.jvlealc.marketsphere.orders.application.command.PlaceOrderCommand;
import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderItemDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderSummaryOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderDetailsByIdQuery;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderSummaryByIdQuery;
import org.springframework.stereotype.Component;

@Component
public class OrderRestMapper {

    public PlaceOrderCommand toPlaceOrderCommand(PlaceOrderRequest request) {
        return new PlaceOrderCommand(
                request.customerId(),
                toPaymentInfoCommand(request.paymentInfo()),
                request.orderItems()
                        .stream()
                        .map(this::toOrderItemCommand)
                        .toList()
        );
    }

    public GetOrderDetailsByIdQuery toDetailsQuery(Long orderId) {
        return new GetOrderDetailsByIdQuery(orderId);
    }

    public GetOrderSummaryByIdQuery toSummaryQuery(Long orderId) {
        return new GetOrderSummaryByIdQuery(orderId);
    }

    public OrderDetailsResponse toDetailsResponse(OrderDetailsOutput output) {
        return new OrderDetailsResponse(
                output.orderId(),
                toCustomerResponse(output.customer()),
                output.orderDate(),
                output.paidAt(),
                output.billedAt(),
                output.shippedAt(),
                output.orderTotal(),
                output.orderStatus(),
                output.orderObservations(),
                output.invoiceUrl(),
                output.trackingCode(),
                output.orderItems().stream()
                        .map(this::toOrderItemDetailsResponse)
                        .toList()
        );
    }

    public OrderSummaryResponse toSummaryResponse(OrderSummaryOutput output) {
        return new OrderSummaryResponse(
                output.id(),
                output.customerId(),
                output.orderDate(),
                output.observations(),
                output.status(),
                output.total(),
                output.amountItems()
        );
    }

    private OrderItemCommand toOrderItemCommand(OrderItemRequest item) {
        return new OrderItemCommand(item.productId(), item.amount());
    }

    private PaymentInfoCommand toPaymentInfoCommand(PaymentInfoRequest paymentInfo) {
        return new PaymentInfoCommand(paymentInfo.metadata(), paymentInfo.paymentType());
    }

    private OrderCustomerResponse toCustomerResponse(CustomerProfile customer) {
        return new OrderCustomerResponse(
                customer.customerId(),
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
                customer.country(),
                customer.active()
        );
    }

    private OrderItemDetailsResponse toOrderItemDetailsResponse(OrderItemDetailsOutput output) {
        return new OrderItemDetailsResponse(
                output.productId(),
                output.productName(),
                output.amount(),
                output.unitPrice(),
                output.active()
        );
    }
}
