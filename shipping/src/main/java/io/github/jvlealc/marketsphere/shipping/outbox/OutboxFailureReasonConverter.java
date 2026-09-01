package io.github.jvlealc.marketsphere.shipping.outbox;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
class OutboxFailureReasonConverter implements AttributeConverter<OutboxFailureReason, String> {

    @Override
    public String convertToDatabaseColumn(OutboxFailureReason attribute) {
        return attribute == null ? null : attribute.value();
    }

    @Override
    public OutboxFailureReason convertToEntityAttribute(String dbData) {
        return dbData == null ? null : OutboxFailureReason.of(dbData);
    }
}
