package io.github.jvlealc.marketsphere.orders.infrastructure.adapter.inbound.kafka;

import io.github.jvlealc.marketsphere.orders.application.EventLineage;
import io.github.jvlealc.marketsphere.orders.infrastructure.kafka.EventHeaders;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

public final class KafkaEventLineageMapper {

    private KafkaEventLineageMapper() {
    }

    public static EventLineage nextEventLineageFrom(ConsumerRecord<String, String> record) {
        return EventLineage.from(
                headerValue(record, EventHeaders.CORRELATION_ID),
                headerValue(record, EventHeaders.EVENT_ID)
        );
    }

    public static String headerValue(ConsumerRecord<String, String> record, String key) {
        Header header = record.headers().lastHeader(key);

        if (header == null || header.value() == null) return null;

        return new String(header.value(), StandardCharsets.UTF_8);
    }
}
