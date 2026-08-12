-- DDL do DB market_sphere_billing
-- Tabela invoices
create table invoices (
    id uuid not null,
    order_id bigint not null,
    status varchar(30) not null default 'PROCESSING',
    storage_key text,
    generated_at timestamp with time zone,
    failed_at timestamp with time zone,
    failure_reason varchar(2000),

    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),

    version bigint not null default 0,

    constraint pk_invoices primary key (id),
    constraint uq_invoices_order_id unique (order_id),
    constraint uq_invoices_storage_key unique (storage_key),

    constraint chk_invoices_order_id_positive check (order_id > 0),

    constraint chk_invoices_status check (
        status in ('PROCESSING', 'FAILED', 'GENERATED')
    ),

    constraint chk_invoices_failed_fields check (
        (
            status = 'FAILED'
            and failed_at is not null
            and failure_reason is not null
            and btrim(failure_reason) <> ''
        )
        or
        (
            status <> 'FAILED'
            and failed_at is null
            and failure_reason is null
        )
    ),

    constraint chk_invoices_generated_fields check (
        (
            status = 'GENERATED'
            and generated_at is not null
            and storage_key is not null
            and btrim(storage_key) <> ''
        )
        or
        (
            status <> 'GENERATED'
            and generated_at is null
            and storage_key is null
        )
    )
);

-- Tabela para OutBox de Billing
create table outbox_messages (
    -- Identidade da linha e, ao mesmo tempo, o eventId publicado no header `payload-id`.
    -- Gerado como UUIDv7 (RFC 9562) na aplicação: ordenável por tempo, o que evita a fragmentação de
    -- índice do v4 aleatório numa tabela que é insert-heavy e sofre vários UPDATE por linha.
    -- Consequência: este UUID é contrato externo — serviços a jusante o gravam como causation_id deles.
    id uuid not null,

    -- Metadados do evento
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100) not null,
    event_type varchar(100) not null,
    event_version integer not null,
    occurred_at timestamp with time zone not null,

    -- Destino lógico da mensagem
    channel varchar(50) not null,

    -- Chave de particionamento do Kafka, distinta da identidade do agregado.
    -- Para ORDER_BILLED: o orderId, chave de negócio pela qual os eventos de um pedido se ordenam.
    message_key varchar(200),

    -- Rastreamento do fluxo distribuído.
    -- Quando o OpenTelemetry entrar, correlation_id passa a ser populado a partir do trace-id — nunca a
    -- conviver com ele como segunda fonte de verdade.
    correlation_id varchar(64),
    causation_id varchar(64),

    -- Conteúdo congelado no momento da transação
    payload jsonb not null,

    -- Estado de processamento da outbox
    status varchar(30) not null default 'PENDING',
    attempts int not null default 0,
    max_attempts int not null default 5,

    -- Idempotência da criação da mensagem na outbox
    idempotency_key varchar(200) not null,

    -- Agendamento e lease do worker.
    -- lock_token identifica QUEM detém o lease. Sem ele, um worker que travou e voltou depois do
    -- locked_until expirar ainda concluiria a mensagem de outro. Podendo gerar perda silenciosa num caso,
    -- evento duplicado no outro.
    next_attempt_at timestamp with time zone default now(),
    locked_until timestamp with time zone,
    lock_token uuid,

    -- Resultado do processamento
    processed_at timestamp with time zone,
    failure_reason varchar(2000),

    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),

    constraint pk_outbox_messages primary key (id),
    constraint uq_outbox_idempotency_key unique (idempotency_key),

    constraint chk_outbox_aggregate_type check (
        aggregate_type in ('INVOICE')
    ),
    constraint chk_outbox_event_type check (
        event_type in ('ORDER_BILLED')
    ),
    constraint chk_outbox_event_version check (
        event_version > 0
    ),
    constraint chk_outbox_status check (
        status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD')
    ),
    constraint chk_outbox_channel check (
        channel in ('EMAIL', 'MESSAGING')
    ),
    constraint chk_outbox_attempts check (
        attempts >= 0
        and max_attempts > 0
        and attempts <= max_attempts
    ),
    constraint chk_outbox_message_key check (
        (
            channel = 'MESSAGING'
            and message_key is not null
            and btrim(message_key) <> ''
        )
        or
        (
            channel = 'EMAIL'
            and message_key is null
        )
    ),
    constraint chk_outbox_lock check (
        (
            status = 'PROCESSING'
            and locked_until is not null
            and lock_token is not null
        )
        or
        (
            status <> 'PROCESSING'
            and locked_until is null
            and lock_token is null
        )
    ),
    constraint chk_outbox_processed_at check (
        (status = 'PROCESSED' and processed_at is not null)
        or
        (status <> 'PROCESSED' and processed_at is null)
    ),
    constraint chk_outbox_next_attempt_at check (
        (status in ('PENDING', 'FAILED') and next_attempt_at is not null)
        or
        (
            status in ('PROCESSING', 'PROCESSED', 'DEAD')
            and next_attempt_at is null
        )
    ),
    constraint chk_outbox_payload_object check (
        jsonb_typeof(payload) = 'object'
    ),
    constraint chk_outbox_failure_reason check (
        (
            status in ('FAILED', 'DEAD')
            and failure_reason is not null
            and btrim(failure_reason) <> ''
        )
        or
        (
            status not in ('FAILED', 'DEAD')
            and failure_reason is null
        )
    ),
    constraint chk_outbox_aggregate_id_not_blank check (
        btrim(aggregate_id) <> ''
    ),
    constraint chk_outbox_idempotency_key_not_blank check (
        btrim(idempotency_key) <> ''
    ),
    constraint chk_outbox_correlation_id_not_blank check (
        correlation_id is null or btrim(correlation_id) <> ''
    ),
    constraint chk_outbox_causation_id_not_blank check (
        causation_id is null or btrim(causation_id) <> ''
    )
);

-- Mensagens aguardando tentativa.
create index idx_outbox_messages_ready
    on outbox_messages (
        channel,
        event_type,
        next_attempt_at,
        created_at
    ) where status in ('PENDING', 'FAILED');

-- Mensagens com lease expirado, reivindicáveis por outro worker.
create index idx_outbox_messages_expired_processing
    on outbox_messages (
        channel,
        event_type,
        locked_until,
        created_at
    ) where status = 'PROCESSING';

create index idx_outbox_messages_aggregate
    on outbox_messages (aggregate_type, aggregate_id);

-- Auditoria por fluxo: select * from outbox_messages where correlation_id = :id order by occurred_at.
create index idx_outbox_messages_correlation
    on outbox_messages (correlation_id)
    where correlation_id is not null;
