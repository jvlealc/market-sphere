package io.github.jvlealc.marketsphere.orders.application.ports.out.outbox;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxRepositoryPort {

    void save(OutboxMessage message);

    List<OutboxMessage> claimProcessableMessages(
            OutboxChannel channel,
            OutboxEventType eventType,
            int limit,
            Duration lockDuration
    );

    void markAsProcessed(UUID messageId);

    void markAsFailed(
            UUID messageId,
            OutboxFailureReason failureReason,
            Duration retryDelay
    );
}
