-- DDL do DB market_sphere_shipping
create extension if not exists pgcrypto;

create table shipments (
    id uuid not null default gen_random_uuid(),
    order_id bigint not null,
    status varchar(30) not null default 'PREPARING_SHIPMENT',
    billed_at timestamp with time zone not null,
    shipped_at timestamp with time zone,
    canceled_at timestamp with time zone,
    tracking_code varchar(120),
    carrier varchar(100),
    customer_id bigint not null,
    correlation_id varchar(64) not null,
    customer_email varchar(150) not null,
    customer_name varchar(200) not null,
    shipment_email_sent_at timestamp with time zone,
    shipment_email_attempts int not null default 0,
    shipment_email_next_attempt_at timestamp with time zone,
    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),

    -- Lock Otimista
    version bigint not null default 0,

    constraint pk_shipments primary key (id),
    constraint uq_shipments_order_id unique (order_id),

    constraint chk_shipments_status check (
        status in ('PREPARING_SHIPMENT', 'CANCELED', 'SHIPPED')
    ),

    constraint chk_shipments_shipped_data check (
        case
            when status = 'SHIPPED'
                then tracking_code is not null and btrim(tracking_code) <> ''
                    and carrier is not null and btrim(carrier) <> ''
                    and shipped_at is not null
            else tracking_code is null and carrier is null and shipped_at is null
        end
    ),

    constraint chk_shipments_canceled_data check (
        (
            status = 'CANCELED'
            and canceled_at is not null
            and (
                tracking_code is null
                and carrier is null
                and shipped_at is null
            )
        )
        or
        (status <> 'CANCELED' and canceled_at is null)
    ),

    constraint chk_shipments_order_id check (order_id > 0),
    constraint chk_shipments_customer_id check (customer_id > 0),

    constraint chk_shipments_correlation_id check (btrim(correlation_id) <> '')
);

create index idx_shipments_status on shipments (status);


create table shipment_events (
    id bigserial not null,
    shipment_id uuid not null,
    shipment_status varchar(30) not null, -- tipo de evento
    description text,
    occurred_at timestamp with time zone default now(),

    constraint pk_shipment_events primary key (id),
    constraint fk_shipment_events_shipment_id foreign key (shipment_id) references shipments(id) on delete cascade,

    constraint chk_shipment_events_shipment_status check (
        shipment_status in ('PREPARING_SHIPMENT', 'CANCELED', 'SHIPPED')
    )
);

create index idx_shipment_events_shipment_id on shipment_events (shipment_id);

-- Tabela para OutBox de Shipping
create table outbox_messages (
    -- Gerado como UUIDv7 (RFC 9562) na aplicação.
    id uuid not null,

    -- Metadados do evento
    aggregate_id varchar(100) not null,
    event_type varchar(100) not null,
    event_version integer not null,
    occurred_at timestamp with time zone not null,

    -- Chave de particionamento do Kafka, distinta da identidade do agregado (orderId)
    message_key varchar(200) not null,

    -- Rastreamento do fluxo distribuído
    correlation_id varchar(64) not null,
    causation_id varchar(64),

    -- Conteúdo congelado no momento da transação. É o contrato publicado verbatim
    payload jsonb not null,

    -- Estado de processamento da outbox
    status varchar(30) not null default 'PENDING',
    attempts int not null default 0,
    max_attempts int not null default 10,

    -- Idempotência da criação da mensagem na outbox
    idempotency_key varchar(200) not null,

    -- Agendamento e lease do worker.
    next_attempt_at timestamp with time zone default now(),

    -- Resultado do processamento
    processed_at timestamp with time zone,
    failure_reason varchar(2000),

    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),

    constraint pk_outbox_messages primary key (id),
    constraint uq_outbox_idempotency_key unique (idempotency_key),

    constraint chk_outbox_event_type check (
        event_type in ('ORDER_PREPARING_SHIPMENT', 'ORDER_SHIPPED')
    ),

    constraint chk_outbox_event_version check (event_version > 0),

    constraint chk_outbox_status check (
        status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD')
    ),

    constraint chk_outbox_attempts check (
        attempts >= 0
        and max_attempts > 0
        and attempts <= max_attempts
    ),

    constraint chk_outbox_processed_at check (
        (status = 'PROCESSED' and processed_at is not null)
        or
        (status <> 'PROCESSED' and processed_at is null)
    ),

    -- PROCESSING carrega o prazo da reivindicaçao: passado ele, a linha volta a ser reivindicável,
    -- e e assim que uma mensagem cujo worker morreu no meio da publicacao nao fica presa.
    constraint chk_outbox_next_attempt_at check (
        (status in ('PENDING', 'FAILED', 'PROCESSING') and next_attempt_at is not null)
        or
        (status in ('PROCESSED', 'DEAD') and next_attempt_at is null)
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

    constraint chk_outbox_message_key_not_blank check (
        btrim(message_key) <> ''
    ),

    constraint chk_outbox_idempotency_key_not_blank check (
        btrim(idempotency_key) <> ''
    ),

    constraint chk_outbox_correlation_id_not_blank check (
        btrim(correlation_id) <> ''
    ),

    constraint chk_outbox_causation_id_not_blank check (
        causation_id is null or btrim(causation_id) <> ''
    )
);

-- Mensagens aguardando tentativa.
create index idx_outbox_messages_ready
    on outbox_messages (next_attempt_at, created_at)
    where status in ('PENDING', 'FAILED', 'PROCESSING');

-- Auditoria por fluxo: select * from outbox_messages where correlation_id = :id order by occurred_at.
create index idx_outbox_messages_correlation
    on outbox_messages (correlation_id);

