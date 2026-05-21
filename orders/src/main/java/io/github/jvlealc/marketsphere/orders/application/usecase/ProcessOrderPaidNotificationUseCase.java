package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.exception.InvalidOutboxMessageException;
import io.github.jvlealc.marketsphere.orders.application.output.OrderDetailsOutput;
import io.github.jvlealc.marketsphere.orders.application.ports.out.customer.CustomerProfile;
import io.github.jvlealc.marketsphere.orders.application.ports.out.notification.NotificationPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.notification.OrderPaidCustomerNotification;
import io.github.jvlealc.marketsphere.orders.application.ports.out.notification.OrderPaidNotification;
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
public class ProcessOrderPaidNotificationUseCase {

    private static final int BATCH_SIZE = 30;
    private static final Duration LOCK_DURATION =  Duration.ofSeconds(150);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(60);

    private final OutboxRepositoryPort outboxRepository;
    private final GetOrderDetailsUseCase getOrderDetailsUseCase;
    private final NotificationPort notificationPort;

    public void execute() {
        List<OutboxMessage> processableMessages = outboxRepository.claimProcessableMessages(
                OutboxChannel.EMAIL,
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
            Long orderId = OutboxAggregateIdsParser.parseOrderId(message.getAggregateId());;
            OrderDetailsOutput output = getOrderDetailsUseCase.execute(
                    new GetOrderDetailsByIdQuery(orderId)
            );

            OrderPaidNotification notification = toOrderPaidNotification(output);

            notificationPort.sendPaidOrderConfirmation(notification);
            outboxRepository.markAsProcessed(message.getId());

            log.info(
                    "[ProcessOrderPaidNotificationUseCase] ORDER_PAID notification sent successfully. outboxId={}, orderId={}",
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
                    "[ProcessOrderPaidNotificationUseCase] Failed to send ORDER_PAID notification. outboxId={}, error={}",
                    message.getId(),
                    exception.getMessage()
            );
        } catch (Exception failureUpdateException) {
            log.error(
                    "[ProcessOrderPaidNotificationUseCase] Could not mark outbox message as failed. outboxId={}, originalError={}, updateError={}",
                    message.getId(),
                    exception.getMessage(),
                    failureUpdateException.getMessage(),
                    failureUpdateException
            );
        }
    }

    private static OrderPaidNotification toOrderPaidNotification(OrderDetailsOutput output) {
        requireNonNull(output, "Order details is required");
        return new OrderPaidNotification(
                output.orderId(),
                output.orderTotal(),
                toCustomerNotification(output.customer())
        );
    }

    private static OrderPaidCustomerNotification toCustomerNotification(CustomerProfile customer) {
        requireNonNull(customer, "Customer data is required");
        return new OrderPaidCustomerNotification(
                customer.customerId(),
                customer.fullName(),
                customer.email()
        );
    }
}
