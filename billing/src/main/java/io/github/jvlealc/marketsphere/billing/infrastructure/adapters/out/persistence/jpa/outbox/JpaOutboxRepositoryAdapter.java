package io.github.jvlealc.marketsphere.billing.infrastructure.adapters.out.persistence.jpa.outbox;

import io.github.jvlealc.marketsphere.billing.application.model.outbox.ClaimedOutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxChannel;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxEventType;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxFailureReason;
import io.github.jvlealc.marketsphere.billing.application.model.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.billing.application.ports.out.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Component
@RequiredArgsConstructor
class JpaOutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository springDataOutboxRepository;
    private final OutboxMessageJpaEntityMapper outboxJpaEntityMapper;

    @Transactional
    @Override
    public void appendNew(OutboxMessage message) {
        requireNonNull(message, "Outbox message must not be null");
        springDataOutboxRepository.save(outboxJpaEntityMapper.toEntity(message));
    }

    @Transactional
    @Override
    public List<ClaimedOutboxMessage> claimProcessableMessages(OutboxChannel channel, OutboxEventType eventType,
                                                               int limit, Duration lockDuration) {
        requireNonNull(channel, "Outbox channel must not be null");
        requireNonNull(eventType, "Outbox event type must not be null");
        requireNonNull(lockDuration, "Lock duration must not be null");
        if (limit <= 0) throw new IllegalArgumentException("Limit must be greater than zero");

        long lockSeconds = lockDuration.toSeconds();

        if (lockSeconds <= 0L) throw new IllegalArgumentException("Lock duration must be greater than zero");

        List<OutboxMessageJpaEntity> entities = springDataOutboxRepository.claimProcessableMessages(
                channel.name(),
                eventType.name(),
                limit,
                lockSeconds
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

        return springDataOutboxRepository.markAsProcessed(messageId, lockToken) > 0;
    }

    @Transactional
    @Override
    public boolean recordFailure(UUID messageId, UUID lockToken, OutboxFailureReason failureReason,
                                 Duration retryDelay) {
        requireNonNull(messageId, "Message ID must not be null");
        requireNonNull(lockToken, "Lock token must not be null");
        requireNonNull(failureReason, "Failure reason must not be null");
        requireNonNull(retryDelay, "Retry delay must not be null");

        long retryDelaySeconds = retryDelay.toSeconds();

        if (retryDelaySeconds <= 0L) throw new IllegalArgumentException("Retry delay must be at least one second");

        return springDataOutboxRepository.recordFailure(
                messageId,
                lockToken,
                failureReason.value(),
                retryDelaySeconds
        ) > 0;
    }

    @Transactional
    @Override
    public boolean markAsDead(UUID messageId, UUID lockToken, OutboxFailureReason failureReason) {
        requireNonNull(messageId, "Message ID must not be null");
        requireNonNull(lockToken, "Lock token must not be null");
        requireNonNull(failureReason, "Failure reason must not be null");

        return springDataOutboxRepository.markAsDead(messageId, lockToken, failureReason.value()) > 0;
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
