package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.command.HandlePaymentConfirmationCommand;
import io.github.jvlealc.marketsphere.orders.application.exception.OrderNotFoundException;
import io.github.jvlealc.marketsphere.orders.application.factory.OrderOutboxMessageFactory;
import io.github.jvlealc.marketsphere.orders.application.messaging.EventLineage;
import io.github.jvlealc.marketsphere.orders.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OrderRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.domain.model.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class HandlePaymentConfirmationUseCase {

    private final OrderRepositoryPort orderRepository;
    private final OutboxRepositoryPort outboxRepository;
    private final OrderOutboxMessageFactory outboxFactory;

    @Transactional
    public void execute(HandlePaymentConfirmationCommand command) {
        Objects.requireNonNull(command, "Payment confirmation command is required");

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

        boolean isPaid = order.markAsPaid(command.paidAt());

        if (!isPaid) {
            return;
        }

        EventLineage eventLineage = EventLineage.startCausedBy(command.paymentEventId());

        OutboxMessage messagingOrderPaidMessage = outboxFactory.createForOrderPaidMessaging(order, eventLineage);
        OutboxMessage emailOrderPaidMessage = outboxFactory.createForOrderPaidNotification(order, eventLineage);

        orderRepository.save(order);
        outboxRepository.appendNew(messagingOrderPaidMessage);
        outboxRepository.appendNew(emailOrderPaidMessage);
    }
}
