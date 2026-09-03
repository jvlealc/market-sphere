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
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class JpaOutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final SpringDataOutboxRepository springDataOutboxRepository;
    private final OutboxMessageJpaEntityMapper outboxJpaEntityMapper;

    @Transactional
    @Override
    public void appendNew(OutboxMessage message) {
        Objects.requireNonNull(message, "message must not be null");
        springDataOutboxRepository.save(outboxJpaEntityMapper.toEntity(message));
    }

    @Transactional
    @Override
    public List<ClaimedOutboxMessage> claimProcessableMessages(OutboxChannel channel, OutboxEventType eventType,
                                                               int limit, Duration lockDuration) {
        Objects.requireNonNull(channel, "channel must not be null");
        Objects.requireNonNull(eventType, "eventType must not be null");
        Objects.requireNonNull(lockDuration, "lockDuration must not be null");
        if (limit <= 0) throw new IllegalArgumentException("limit must be greater than zero");

        long lockSeconds = lockDuration.toSeconds();

        if (lockSeconds <= 0L) throw new IllegalArgumentException("lockDuration must be greater than zero");

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
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(lockToken, "lockToken must not be null");

        return springDataOutboxRepository.markAsProcessed(messageId, lockToken) > 0;
    }

    @Transactional
    @Override
    public boolean recordFailure(UUID messageId, UUID lockToken, OutboxFailureReason failureReason,
                                 Duration retryDelay) {
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(lockToken, "lockToken must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");
        Objects.requireNonNull(retryDelay, "retryDelay must not be null");

        long retryDelaySeconds = retryDelay.toSeconds();

        if (retryDelaySeconds <= 0L) throw new IllegalArgumentException("retryDelay must be at least one second");

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
        Objects.requireNonNull(messageId, "messageId must not be null");
        Objects.requireNonNull(lockToken, "lockToken must not be null");
        Objects.requireNonNull(failureReason, "failureReason must not be null");

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
