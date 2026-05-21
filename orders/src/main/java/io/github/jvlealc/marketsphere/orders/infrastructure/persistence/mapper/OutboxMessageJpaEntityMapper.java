package io.github.jvlealc.marketsphere.orders.infrastructure.persistence.mapper;

import io.github.jvlealc.marketsphere.orders.application.ports.out.outbox.OutboxMessage;
import io.github.jvlealc.marketsphere.orders.infrastructure.persistence.entity.OutboxMessageJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class OutboxMessageJpaEntityMapper {

    public OutboxMessageJpaEntity toEntity(OutboxMessage message) {
        OutboxMessageJpaEntity entity = new OutboxMessageJpaEntity();

        entity.setId(message.getId());
        entity.setAggregateType(message.getAggregateType());
        entity.setAggregateId(message.getAggregateId());
        entity.setEventType(message.getEventType());
        entity.setChannel(message.getChannel());
        entity.setPayload(message.getPayload());
        entity.setStatus(message.getStatus());
        entity.setAttempts(message.getAttempts());
        entity.setMaxAttempts(message.getMaxAttempts());
        entity.setNextAttemptAt(message.getNextAttemptAt());
        entity.setIdempotencyKey(message.getIdempotencyKey());
        entity.setErrorMessage(message.getErrorMessage());

        return entity;
    }

    public OutboxMessage toApplicationModel(OutboxMessageJpaEntity entity) {
        return OutboxMessage.rehydrate(
                entity.getId(),
                entity.getAggregateType(),
                entity.getAggregateId(),
                entity.getEventType(),
                entity.getChannel(),
                entity.getPayload(),
                entity.getStatus(),
                entity.getAttempts(),
                entity.getMaxAttempts(),
                entity.getNextAttemptAt(),
                entity.getIdempotencyKey(),
                entity.getErrorMessage()
        );
    }
}
