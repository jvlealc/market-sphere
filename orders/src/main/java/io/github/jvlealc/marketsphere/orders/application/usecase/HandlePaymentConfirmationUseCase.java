package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandlePaymentConfirmationCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidCommandException;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.exception.InvalidQueryException;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.*;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class HandlePaymentConfirmationUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;

    @Transactional
    public void execute(HandlePaymentConfirmationCommand command) {
        if (command == null ) {
            throw new InvalidCommandException("Payment confirmation command is required");
        }

        if (command.orderId() == null || command.paymentKey() == null || command.paymentKey().isBlank()) {
            throw new InvalidCommandException("Order ID and payment key are required");
        }

        Order order = orderRepository.findByIdAndPaymentKey(command.orderId(), command.paymentKey())
                .orElseThrow(() -> new OrderNotFoundException(
                        "Not found order with ID '%s' and payment-key '%s' ".formatted(command.orderId(), command.paymentKey())
                ));

        if (!command.successful()) {
            boolean isFailed = order.markPaymentAsFailed(command.observations());
            if (isFailed) {
                orderRepository.save(order);
            }
            return;
        }

        boolean isPaid = order.markAsPaid(
                (command.paidAt() != null)
                        ? command.paidAt()
                        : Instant.now()
        );

        if (isPaid) {
            OutboxMessage messagingOrderPaidMessage = createOrderPaidOutboxMessage(order.getId(), OutboxChannel.MESSAGING);
            OutboxMessage emailOrderPaidMessage = createOrderPaidOutboxMessage(order.getId(), OutboxChannel.EMAIL);

            orderRepository.save(order);
            outboxRepository.save(messagingOrderPaidMessage);
            outboxRepository.save(emailOrderPaidMessage);
        }
    }

    private static OutboxMessage createOrderPaidOutboxMessage(Long orderId, OutboxChannel channel) {
        String payload = """
            {
               "orderId": %d
            }
            """.formatted(orderId);

        String idempotencyKey = channel.name().toLowerCase() + "-order-paid-order-" + orderId;

        return OutboxMessage.createNew(
                OutboxAggregateType.ORDER,
                orderId.toString(),
                OutboxEventType.ORDER_PAID,
                channel,
                payload,
                idempotencyKey
        );
    }
}
