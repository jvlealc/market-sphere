package io.github.jvlealc.marketsphere.billing.infrastructure.messaging;

/**
 * Nomes dos headers que compõem o envelope de um evento no Kafka.
 *
 * <h5>Por que headers, e não campos do payload</h5>
 * O payload é congelado na transação de negócio e publicado verbatim — é o contrato. Metadado que descreve
 * o evento, e não o fato, vai no envelope: assim é possível acrescentar rastreamento sem alterar o conteúdo
 * de linhas de outbox gravadas antes da mudança. Um consumidor também consegue rotear por tipo e versão sem
 * desserializar o corpo.
 *
 * <h5>Sobre o {@code correlation-id}</h5>
 * É o identificador do fluxo. Quando o OpenTelemetry entrar, ele passa a ser <em>populado a partir</em> do
 * {@code trace-id} do {@code traceparent} do W3C Trace Context.
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
