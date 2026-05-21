package io.github.jvlealc.marketsphere.orders.infrastructure.persistence;

import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.*;
import io.github.jvlealc.marketsphere.orders.infrastructure.exception.OutboxPersistenceException;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OutboxMessageJpaEntity;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper.OutboxMessageJpaEntityMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class OutboxJpaRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository springDataOutboxRepository;
    private final OutboxMessageJpaEntityMapper outboxMessageJpaEntityMapper;

    @Override
    public void save(OutboxMessage message) {
        requireNonNull(message, "Outbox message must not be null");
        springDataOutboxRepository.save(
                outboxMessageJpaEntityMapper.toEntity(message)
        );
    }

    @Transactional
    @Override
    public List<OutboxMessage> claimProcessableMessages(OutboxChannel channel, OutboxEventType eventType,
                                                   int limit, Duration lockDuration) {
        requireNonNull(channel, "channel must not be null");
        requireNonNull(eventType, "eventType must not be null");
        requireNonNull(lockDuration, "lockDuration must not be null");

        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than zero");
        }
        if (lockDuration.isZero() || lockDuration.isNegative()) {
            throw new IllegalArgumentException("lockDuration must be greater than zero");
        }

        List<OutboxMessageJpaEntity> entities = springDataOutboxRepository.claimProcessableMessages(
                channel.name(),
                eventType.name(),
                limit,
                lockDuration.toSeconds()
        );

        return entities.stream()
                .map(outboxMessageJpaEntityMapper::toApplicationModel)
                .toList();
    }

    @Transactional
    @Override
    public void markAsProcessed(UUID messageId) {
        requireNonNull(messageId, "messageId must not be null");

        int updatedRows = springDataOutboxRepository.markAsProcessed(messageId);

        if (updatedRows == 0) {
            throw new OutboxPersistenceException(
                    "Could not mark outbox message as 'PROCESSED'. Message was not found or is not PROCESSING. ID: " + messageId
            );
        }
    }

    @Override
    public void markAsFailed(UUID messageId, OutboxFailureReason failureReason, Duration retryDelay) {
        requireNonNull(messageId, "messageId must not be null");
        requireNonNull(failureReason, "failureReason must not be null");
        requireNonNull(retryDelay, "retryDelay must not be null");

        if (retryDelay.isNegative()) {
            throw new IllegalArgumentException("retryDelay must be greater than zero");
        }

        int updatedRows = springDataOutboxRepository.markAsFailed(messageId, failureReason.value(), retryDelay.toSeconds());

        if (updatedRows == 0) {
            throw new OutboxPersistenceException(
                    "Could not mark outbox message as 'FAILED'. Message was not found or is not PROCESSING. ID: " + messageId
            );
        }
    }
}
