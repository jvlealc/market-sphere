package io.github.jvlealc.marketsphere.orders.infrastructure.adapters.out.persistence.jpa.outbox;

import io.github.jvlealc.marketsphere.orders.application.model.outbox.*;
import io.github.jvlealc.marketsphere.orders.application.ports.out.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
public class OutboxJpaRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository springDataOutboxRepository;
    private final OutboxMessageJpaEntityMapper outboxJpaEntityMapper;

    @Override
    public void appendNew(OutboxMessage message) {
        requireNonNull(message, "Outbox message must not be null");
        springDataOutboxRepository.save(
                outboxJpaEntityMapper.toEntity(message)
        );
    }

    @Transactional
    @Override
    public List<ClaimedOutboxMessage> claimProcessableMessages(
            OutboxChannel channel,
            OutboxEventType eventType,
            int limit,
            Duration lockDuration
    ) {
        requireNonNull(channel, "channel must not be null");
        requireNonNull(eventType, "eventType must not be null");
        requireNonNull(lockDuration, "lockDuration must not be null");

        if (limit <= 0) throw new IllegalArgumentException("limit must be greater than zero");

        long lockDurationSeconds = lockDuration.toSeconds();

        if (lockDurationSeconds <= 0L) throw new IllegalArgumentException("lockDuration must be greater than zero");

        List<OutboxMessageJpaEntity> entities = springDataOutboxRepository.claimProcessableMessages(
                channel.name(),
                eventType.name(),
                limit,
                lockDurationSeconds
        );

        return entities.stream()
                .map(this::toClaimedMessage)
                .toList();
    }

    @Transactional
    @Override
    public boolean markAsProcessed(UUID messageId, UUID lockToken) {
        requireNonNull(messageId, "Message ID must not be null");
        requireNonNull(lockToken, "Lock token must not be null");

        int updatedRows = springDataOutboxRepository.markAsProcessed(messageId,  lockToken);

        return updatedRows == 1;
    }

    @Transactional
    @Override
    public boolean recordFailure(UUID messageId, UUID lockToken, OutboxFailureReason failureReason, Duration retryDelay) {
        requireNonNull(messageId, "Message ID must not be null");
        requireNonNull(lockToken, "Lock token must not be null");
        requireNonNull(failureReason, "Failure reason must not be null");
        requireNonNull(retryDelay, "Retry delay must not be null");

        long retryDelaySeconds = retryDelay.getSeconds();

        if (retryDelaySeconds <= 0L) {
            throw new IllegalArgumentException("Retry delay must be greater than zero");
        }

        int updatedRows = springDataOutboxRepository.recordFailure(
                messageId,
                lockToken,
                failureReason.value(),
                retryDelay.toSeconds()
        );

        return updatedRows == 1;
    }

    @Transactional
    @Override
    public boolean markAsDead(UUID messageId, UUID lockToken, OutboxFailureReason failureReason) {
        requireNonNull(messageId, "Message ID must not be null");
        requireNonNull(lockToken, "Lock token must not be null");
        requireNonNull(failureReason, "Failure reason must not be null");

        int updatedRows = springDataOutboxRepository.markAsDead(messageId, lockToken, failureReason.value());

        return updatedRows == 1;
    }

    /**
     * O token vem da entidade recém-reivindicada, não do modelo: {@code OutboxMessage} descreve o evento, e
     * o lease é de quem o está processando agora.
     */
    private ClaimedOutboxMessage toClaimedMessage(OutboxMessageJpaEntity entity) {
        return new ClaimedOutboxMessage(
                outboxJpaEntityMapper.toApplicationModel(entity),
                entity.getLockToken()
        );
    }
}
