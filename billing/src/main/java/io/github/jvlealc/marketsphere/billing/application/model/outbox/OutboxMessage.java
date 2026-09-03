package io.github.jvlealc.marketsphere.billing.application.model.outbox;

import io.github.jvlealc.marketsphere.billing.application.identity.UuidV7;
import io.github.jvlealc.marketsphere.billing.application.model.messaging.EventLineage;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Mensagem da outbox transacional: o envelope de um evento gravado na mesma transação da mudança de estado
 * que o originou.
 *
 * <h2>Envelope e payload</h2>
 * Os metadados aqui — tipo, versão, chave de particionamento, linhagem — descrevem o evento e viajam como
 * <em>headers</em> do Kafka. O {@link SerializedOutboxPayload} é o corpo, congelado no momento da transação e
 * publicado verbatim. A separação é o que permite acrescentar metadado sem alterar o conteúdo de linhas
 * gravadas antes da mudança.
 *
 * <h2>O que este modelo deliberadamente não carrega</h2>
 * {@code lockedUntil} e {@code lockToken} são estados de <em>lease</em> do worker, não do evento. Ficam em
 * {@link ClaimedOutboxMessage}, que é o que a reivindicação devolve.
 */
public final class OutboxMessage {

    private static final int DEFAULT_MAX_ATTEMPTS = 5;

    private final UUID id;
    private final OutboxAggregateType aggregateType;
    private final String aggregateId;
    private final OutboxEventType eventType;
    private final int eventVersion;
    private final Instant occurredAt;
    private final OutboxChannel channel;
    private final String messageKey;
    private final SerializedOutboxPayload payload;
    private final OutboxStatus status;
    private final int attempts;
    private final int maxAttempts;
    private final Instant nextAttemptAt;
    private final String idempotencyKey;
    private final EventLineage eventLineage;
    private final OutboxFailureReason failureReason;

    private OutboxMessage(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            EventLineage eventLineage,
            OutboxFailureReason failureReason
    ) {
        validateAttemptsConsistency(attempts, maxAttempts);

        this.id = Objects.requireNonNull(id, "id must not be null");
        this.aggregateType = Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        this.aggregateId = requireText(aggregateId, "aggregateId must not be null or blank");
        this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
        this.eventVersion = requirePositiveVersion(eventVersion);
        this.occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
        this.channel = Objects.requireNonNull(channel, "channel must not be null");
        this.messageKey = requireValidMessageKey(this.channel, messageKey);
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.nextAttemptAt = requireValidNextAttemptAt(this.status, nextAttemptAt);
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.idempotencyKey = requireText(idempotencyKey, "idempotencyKey must not be null or blank");
        this.eventLineage = Objects.requireNonNull(eventLineage, "eventLineage must not be null");
        this.failureReason = failureReason;
    }

    public static OutboxMessage createNew(
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            String idempotencyKey,
            EventLineage eventLineage,
            Instant nextAttemptAt
    ) {
        return new OutboxMessage(
                createId(),
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                channel,
                messageKey,
                payload,
                OutboxStatus.PENDING,
                0,
                DEFAULT_MAX_ATTEMPTS,
                nextAttemptAt,
                idempotencyKey,
                eventLineage,
                null
        );
    }

    public static OutboxMessage rehydrate(
            UUID id,
            OutboxAggregateType aggregateType,
            String aggregateId,
            OutboxEventType eventType,
            int eventVersion,
            Instant occurredAt,
            OutboxChannel channel,
            String messageKey,
            SerializedOutboxPayload payload,
            OutboxStatus status,
            int attempts,
            int maxAttempts,
            Instant nextAttemptAt,
            String idempotencyKey,
            EventLineage eventLineage,
            OutboxFailureReason failureReason
    ) {
        return new OutboxMessage(
                id,
                aggregateType,
                aggregateId,
                eventType,
                eventVersion,
                occurredAt,
                channel,
                messageKey,
                payload,
                status,
                attempts,
                maxAttempts,
                nextAttemptAt,
                idempotencyKey,
                eventLineage,
                failureReason
        );
    }

    public UUID getId() {
        return id;
    }

    public OutboxAggregateType getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public OutboxEventType getEventType() {
        return eventType;
    }

    public int getEventVersion() {
        return eventVersion;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public OutboxChannel getChannel() {
        return channel;
    }

    /**
     * Chave de particionamento do Kafka. Nula no canal {@code EMAIL}, onde não existe partição a escolher.
     */
    public String getMessageKey() {
        return messageKey;
    }

    public SerializedOutboxPayload getPayload() {
        return payload;
    }

    public OutboxStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public EventLineage getEventLineage() {
        return eventLineage;
    }

    public OutboxFailureReason getFailureReason() {
        return failureReason;
    }

    private static UUID createId() {
        return UuidV7.generate();
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }

        return value.trim();
    }

    private static int requirePositiveVersion(int eventVersion) {
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be greater than zero");
        }

        return eventVersion;
    }

    private static void validateAttemptsConsistency(int attempts, int maxAttempts) {
        if (attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }

        if (maxAttempts <= 0) {
            throw new IllegalArgumentException("maxAttempts must be greater than zero");
        }

        if (attempts > maxAttempts) {
            throw new IllegalArgumentException("attempts must not be greater than maxAttempts");
        }
    }

    private static String requireValidMessageKey(OutboxChannel channel, String messageKey) {
        return switch (channel) {
            case MESSAGING -> requireText(messageKey, "messageKey must not be null or blank");

            case EMAIL -> {
                if (messageKey != null) {
                    throw new IllegalArgumentException("messageKey must be null for the EMAIL channel");
                }

                yield null;
            }
        };
    }

    private static Instant requireValidNextAttemptAt(OutboxStatus status, Instant nextAttemptAt) {
        return switch (status) {
            case PENDING, FAILED ->
                    Objects.requireNonNull(nextAttemptAt, "nextAttemptAt must not be null for status " + status);

            case PROCESSING, PROCESSED, DEAD -> {
                if (nextAttemptAt != null) {
                    throw new IllegalArgumentException("nextAttemptAt must be null for status " + status);
                }

                yield null;
            }
        };
    }
}
