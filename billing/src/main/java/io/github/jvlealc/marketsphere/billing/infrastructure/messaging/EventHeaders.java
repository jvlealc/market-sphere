package io.github.jvlealc.marketsphere.billing.infrastructure.messaging;

/**
 * Nomes dos headers que compõem o envelope de um evento no Kafka.
 *
 * <h2>Por que headers, e não campos do payload</h2>
 * O payload é congelado na transação de negócio e publicado verbatim — é o contrato. Metadado que descreve
 * o evento, e não o fato, vai no envelope: assim é possível acrescentar rastreamento sem alterar o conteúdo
 * de linhas de outbox gravadas antes da mudança. Um consumidor também consegue rotear por tipo e versão sem
 * desserializar o corpo.
 *
 * <h2>Relação com o CloudEvents</h2>
 * Os nomes aqui são explícitos de propósito — {@code aggregate-id} diz o que é, enquanto o
 * {@code ce_subject} equivalente do CloudEvents é genérico por precisar servir a qualquer domínio. A
 * semântica, porém, é deliberadamente mapeável 1:1: {@code payload-id} → {@code ce_id}, {@code payload-type} →
 * {@code ce_type}, {@code occurred-at} → {@code ce_time}, {@code aggregate-id} → {@code ce_subject}. Se um
 * dia o SDK do CloudEvents entrar, a migração é uma tabela de renomeação, não um redesenho.
 *
 * <h2>Sobre o {@code correlation-id}</h2>
 * É o identificador do fluxo. Quando o OpenTelemetry entrar, ele passa a ser <em>populado a partir</em> do
 * {@code trace-id} do {@code traceparent} do W3C Trace Context — nunca a conviver com ele. Dois mecanismos
 * de correlação no mesmo sistema equivalem a nenhum.
 */
public final class EventHeaders {

    /** Identidade do evento. É o {@code id} da linha de outbox, e vira o {@code causation-id} a jusante. */
    public static final String EVENT_ID = "payload-id";

    public static final String EVENT_TYPE = "payload-type";

    public static final String EVENT_VERSION = "payload-version";

    public static final String AGGREGATE_TYPE = "aggregate-type";

    public static final String AGGREGATE_ID = "aggregate-id";

    /** Quando o fato ocorreu, em ISO-8601. Distinto de quando a mensagem foi publicada. */
    public static final String OCCURRED_AT = "occurred-at";

    public static final String CORRELATION_ID = "correlation-id";

    /** O {@code payload-id} do evento que causou este. Linhagem, não tracing. */
    public static final String CAUSATION_ID = "causation-id";

    public static final String CONTENT_TYPE = "content-type";

    public static final String APPLICATION_JSON = "application/json";

    private EventHeaders() {
    }
}
