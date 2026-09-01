package io.github.jvlealc.marketsphere.shipping.messaging.kafka;

import io.github.jvlealc.marketsphere.shipping.messaging.EventHeaders;
import io.github.jvlealc.marketsphere.shipping.messaging.EventLineage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

import java.nio.charset.StandardCharsets;

public final class EventHeaderReader {

    private EventHeaderReader() {
    }

    /**
     * Linhagem dos eventos que este consumo vai originar: o {@code correlationId} atravessa o fluxo
     * inteiro, e a causa direta é o <strong>{@code event-id} da mensagem consumida.
     */
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
