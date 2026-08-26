package io.github.jvlealc.marketsphere.orders.infrastructure.messaging;

/**
 * Nomes dos headers que compõem o envelope de um evento no Kafka.
 */
public final class EventHeaders {

    /** Identidade do evento. É o {@code id} da linha de outbox, e vira o {@code causation-id} a jusante. */
    public static final String EVENT_ID = "event-id";

    public static final String EVENT_TYPE = "event-type";

    public static final String EVENT_VERSION = "event-version";

    public static final String AGGREGATE_TYPE = "aggregate-type";

    public static final String AGGREGATE_ID = "aggregate-id";

    /** Quando o fato ocorreu, em ISO-8601. Distinto de quando a mensagem foi publicada. */
    public static final String OCCURRED_AT = "occurred-at";

    public static final String CORRELATION_ID = "correlation-id";

    /** O {@code event-id} do evento que causou este. Linhagem, não tracing. */
    public static final String CAUSATION_ID = "causation-id";

    public static final String CONTENT_TYPE = "content-type";

    public static final String APPLICATION_JSON = "application/json";

    private EventHeaders() {
    }
}
