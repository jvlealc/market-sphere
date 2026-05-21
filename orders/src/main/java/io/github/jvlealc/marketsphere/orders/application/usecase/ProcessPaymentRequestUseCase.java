package io.github.jvlealc.marketsphere.orders.application.usecase;

import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxRepositoryPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.payment.PaymentGatewayPort;
import io.github.jvlealc.marketsphere.orders.application.ports.out.payment.PaymentRequestReceipt;
import io.github.jvlealc.marketsphere.orders.application.service.PaymentRequestCompletionService;
import io.github.jvlealc.marketsphere.orders.application.support.OutboxAggregateIdsParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProcessPaymentRequestUseCase {

    private static final int BATCH_SIZE = 10;
    private static final Duration LOCK_DURATION =  Duration.ofSeconds(30);
    private static final Duration RETRY_DELAY = Duration.ofSeconds(15);

    private final OutboxRepositoryPort outboxRepository;
    private final PaymentGatewayPort paymentGateway;
    private final PaymentRequestCompletionService paymentRequestCompletionService;

    public void execute() {
        List<OutboxMessage> processableMessages = outboxRepository.claimProcessableMessages(
                OutboxChannel.PAYMENT,
                OutboxEventType.PAYMENT_REQUEST_REQUIRED,
                BATCH_SIZE,
                LOCK_DURATION
        );

        if (processableMessages.isEmpty()) {
            return;
        }

        processableMessages.forEach(this::processMessage);
    }

    private void processMessage(OutboxMessage message) {
        try {
            Long orderId = OutboxAggregateIdsParser.parseOrderId(message.getAggregateId());
            PaymentRequestReceipt paymentRequestReceipt = paymentGateway.requestPayment(orderId, message.getIdempotencyKey());

            //Delega conclusão transacional
            paymentRequestCompletionService.complete(orderId, paymentRequestReceipt, message);

            log.info(
                    "[ProcessPaymentRequestUseCase] Payment request processed successfully. outboxId={}, orderId={}",
                    message.getId(),
                    orderId
            );

        } catch (Exception e) {
            markMessageAsFailed(message, e);
        }
    }

    private void markMessageAsFailed(OutboxMessage message, Exception exception) {
        try {
            outboxRepository.markAsFailed(
                    message.getId(),
                    OutboxFailureReason.of(exception),
                    RETRY_DELAY
            );

        } catch (Exception failureUpdateException) {
            log.error(
                    "[ProcessPaymentRequestUseCase] Could not mark outbox message as failed. outboxId={}, originalError={}, updateError={}",
                    message.getId(),
                    exception.getMessage(),
                    failureUpdateException.getMessage(),
                    failureUpdateException
            );
        }
    }
}
