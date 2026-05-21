package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.output.OrderItemDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidCustomerPayload;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidEvent;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidItemPayload;
import io.github.jvlealc.marketsphere.orders.application.ports.out.messaging.publisher.OrderPaidPublisherPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.query.GetOrderDetailsByIdQuery;
import io.github.jvlealc.marketsphere.orders.application.support.OutboxAggregateIdsParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessOrderPaidMessagingUseCase {

    private static final int BATCH_SIZE = 20;
    private static final Duration LOCK_DURATION =  Duration.ofSeconds(60);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(10);

    private final OutboxRepositoryPort outboxRepository;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final OrderPaidPublisherPort orderPaidPublisher;

    public void execute() {
        List<OutboxMessage> processableMessages = outboxRepository.claimProcessableMessages(
                OutboxChannel.MESSAGING,
                OutboxEventType.ORDER_PAID,
                BATCH_SIZE,
                LOCK_DURATION
        );

        if (processableMessages == null || processableMessages.isEmpty()) {
            return;
        }

        processableMessages.forEach(this::processMessage);
    }

    private void processMessage(OutboxMessage message) {
        try {
            Long orderId = OutboxAggregateIdsParser.parseOrderId(message.getAggregateId());
            OrderDetailsOutput output = getOrderDetailsUseCase.execute(
                    new GetOrderDetailsByIdQuery(orderId)
            );

            OrderPaidEvent event = toOrderPaidEvent(output);

            orderPaidPublisher.publish(event);
            outboxRepository.markAsProcessed(message.getId());

            log.info(
                    "ORDER_PAID event published successfully. outboxId={}, orderId={}",
                    message.getId(),
                    orderId
            );

        } catch (Exception exception) {
            markMessageAsFailed(message, exception);
        }
    }

    private void markMessageAsFailed(OutboxMessage message, Exception exception) {
        try {
            outboxRepository.markAsFailed(
                    message.getId(),
                    OutboxFailureReason.of(exception),
                    RETRY_DELAY
            );

            log.warn(
                    "Failed to publish ORDER_PAID event. outboxId={}, error={}",
                    message.getId(),
                    exception.getMessage()
            );
        } catch (Exception failureUpdateException) {
            log.error(
                    "Could not mark outbox message as failed. outboxId={}, originalError={}, updateError={}",
                    message.getId(),
                    exception.getMessage(),
                    failureUpdateException.getMessage(),
                    failureUpdateException
            );
        }
    }

    private static OrderPaidEvent toOrderPaidEvent(OrderDetailsOutput output) {
        requireNonNull(output, "Order details is required");
        return new OrderPaidEvent(
                output.orderId(),
                toCustomerPayload(output.customer()),
                output.orderDate(),
                output.orderTotal(),
                output.orderStatus(),
                output.orderObservations(),
                output.orderItems().stream()
                        .map(ProcessOrderPaidMessagingUseCase::toOrderItemPayload)
                        .toList()
        );
    }

    private static OrderPaidCustomerPayload  toCustomerPayload(CustomerProfile customer) {
        requireNonNull(customer, "Customer data is required");
        return new OrderPaidCustomerPayload(
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
                customer.country()
        );
    }

    private static OrderPaidItemPayload toOrderItemPayload(OrderItemDetailsOutput output) {
        requireNonNull(output, "Order item is required");
        return new OrderPaidItemPayload(
                output.productId(),
                output.productName(),
                output.amount(),
                output.unitPrice()
        );
    }
}
