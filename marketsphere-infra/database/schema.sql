--- Bancos de dados
create database market_sphere_products;
create database market_sphere_orders;
create database market_sphere_customers;
create database market_sphere_shipping;
create database market_sphere_billing;



-- DDL do DB market_sphere_customers
-- Tabela customers
create table customers (
    id bigserial not null,
    full_name varchar(200) not null,
    national_id varchar(20) not null,
    email varchar(150) not null,
    phone_number varchar(25) not null,
    active boolean not null default true,

    -- Embedded Address (VO)
    postal_code varchar(20) not null,
    street varchar(100) not null,
    house_number varchar(10) not null,
    complement varchar(50),
    neighborhood varchar(100),
    city varchar(100) not null,
    state varchar(100) not null,
    country varchar(100) not null,

    constraint pk_customers_id primary key (id),
    constraint uq_customers_national_id unique (national_id),
    constraint uq_customers_email unique (email)
);

comment on table customers is 'dados mestres dos clientes, incluindo o objeto de valor Address embutido';

comment on column customers.id is 'chave primária substituta';
comment on column customers.full_name is 'nome legal completo do cliente';
comment on column customers.national_id is 'identificador nacional do cliente, como CPF no Brasil';
comment on column customers.email is 'endereço de e-mail único do cliente';
comment on column customers.phone_number is 'número de telefone do cliente, podendo incluir código do país';

-- Address (VO embutido)
comment on column customers.postal_code is 'código postal do cliente, como ZIP, CEP etc.';
comment on column customers.street is 'nome da rua do cliente';
comment on column customers.house_number is 'número da casa, prédio ou imóvel do cliente';
comment on column customers.complement is 'informações adicionais do endereço, como apartamento, sala etc.';
comment on column customers.neighborhood is 'bairro ou distrito do cliente';
comment on column customers.city is 'cidade do cliente';
comment on column customers.state is 'estado, província ou região do cliente';
comment on column customers.country is 'país do cliente';



-- DDL do DB market_sphere_products
-- Tabela: products
create table products (
    id bigserial not null,
    name varchar(150) not null,
    unit_price decimal(16,2) not null,
    description text not null,
    active boolean not null default true,

    constraint pk_products_id primary key (id)
);

comment on column products.id is 'identificador único do produto';
comment on column products.name is 'nome do produto';
comment on column products.unit_price is 'preço de uma unidade do produto';
comment on column products.description is 'descrição detalhada do produto';
comment on column products.active is 'define se o produto está ativo e disponível';



-- DDL do DB market_sphere_orders

-- Tabela: orders
create table orders (
    id bigserial not null,
    customer_id bigint not null,

    customer_full_name varchar(200) not null,
    customer_national_id varchar(20) not null,
    customer_email varchar(150) not null,
    customer_phone_number varchar(25) not null,
    customer_postal_code varchar(20) not null,
    customer_street varchar(100) not null,
    customer_house_number varchar(10) not null,
    customer_complement varchar(50),
    customer_neighborhood varchar(100),
    customer_city varchar(100) not null,
    customer_state varchar(100) not null,
    customer_country varchar(100) not null,

    order_date timestamp with time zone not null default now(),
    paid_at timestamp with time zone,
    billed_at timestamp with time zone,
    shipped_at timestamp with time zone,

    payment_key text,
    observations varchar(500),
    status varchar(30) not null,
    total decimal(16,2) not null,
    tracking_code varchar(120),
    invoice_id varchar(64),

    -- Lock otimista (@Version). Webhook de pagamento e consumidor de ORDER_BILLED podem tentar atualizar
    -- a linhas concorrentemente.
    version bigint not null default 0,

    constraint pk_orders_id primary key (id),

    -- Uma chave de pagamento identifica UMA transação.
    constraint uq_orders_payment_key unique (payment_key),

    constraint uq_orders_invoice_id unique (invoice_id),

    constraint chk_orders_status check (
        status in ('PAYMENT_PENDING', 'PAID', 'BILLED', 'PREPARING_SHIPMENT', 'SHIPPED', 'PAYMENT_ERROR', 'CANCELED')
    ),

    constraint chk_orders_total check (total >= 0),

    constraint chk_orders_customer_id check (customer_id > 0),

    -- payment_key é implicação, não equivalência: um pedido PAYMENT_PENDING pode ter chave
    -- registrada enquanto aguarda a confirmação do gateway.
    constraint chk_orders_payment_key_required check (
        status not in ('PAID', 'BILLED', 'PREPARING_SHIPMENT', 'SHIPPED')
        or (payment_key is not null and btrim(payment_key) <> '')
    ),

    -- CANCELED fica de fora das equivalências abaixo: um pedido cancelado preserva o que já
    -- tinha, e responde pelas restrições chk_orders_canceled_*.
    constraint chk_orders_paid_at_matches_status check (
        status = 'CANCELED'
        or case
               when status in ('PAID', 'BILLED', 'PREPARING_SHIPMENT', 'SHIPPED')
                   then paid_at is not null
               else paid_at is null
           end
    ),

    constraint chk_orders_billing_fields_match_status check (
        status = 'CANCELED'
        or case
               when status in ('BILLED', 'PREPARING_SHIPMENT', 'SHIPPED')
                   then billed_at is not null
                        and invoice_id is not null
                        and btrim(invoice_id) <> ''
               else billed_at is null
                    and (invoice_id is null or btrim(invoice_id) = '')
           end
    ),

    constraint chk_orders_shipping_fields_match_status check (
        status = 'CANCELED'
        or case
               when status = 'SHIPPED'
                   then shipped_at is not null
                        and tracking_code is not null
                        and btrim(tracking_code) <> ''
               else shipped_at is null
                    and (tracking_code is null or btrim(tracking_code) = '')
           end
    ),

    -- Cancelar a partir de SHIPPED é recusado pelo agregado, então dado de envio aqui é
    -- estado inalcançável, não histórico.
    constraint chk_orders_canceled_has_no_shipping_data check (
        status <> 'CANCELED'
        or (shipped_at is null and (tracking_code is null or btrim(tracking_code) = ''))
    ),

    constraint chk_orders_canceled_billing_is_coherent check (
        status <> 'CANCELED'
        or (billed_at is not null) = (invoice_id is not null and btrim(invoice_id) <> '')
    ),

    constraint chk_orders_canceled_billed_implies_paid check (
        status <> 'CANCELED'
        or billed_at is null
        or paid_at is not null
    ),

    constraint chk_orders_canceled_paid_implies_payment_key check (
        status <> 'CANCELED'
        or paid_at is null
        or (payment_key is not null and btrim(payment_key) <> '')
    )
);

comment on table orders is 'armazena os pedidos dos clientes';
comment on column orders.id is 'chave primária da tabela de pedidos';
comment on column orders.customer_id is 'referência ao cliente que realizou o pedido';
comment on column orders.customer_full_name is 'nome do cliente no momento da compra';
comment on column orders.customer_national_id is 'documento do cliente no momento da compra';
comment on column orders.customer_email is 'e-mail do cliente no momento da compra';
comment on column orders.customer_phone_number is 'telefone do cliente no momento da compra';
comment on column orders.customer_postal_code is 'código postal de entrega no momento da compra';
comment on column orders.customer_street is 'logradouro de entrega no momento da compra';
comment on column orders.customer_house_number is 'número do imóvel de entrega no momento da compra';
comment on column orders.customer_complement is 'complemento do endereço; nullable na origem';
comment on column orders.customer_neighborhood is 'bairro do endereço; nullable na origem';
comment on column orders.customer_city is 'cidade de entrega no momento da compra';
comment on column orders.customer_state is 'estado de entrega no momento da compra';
comment on column orders.customer_country is 'país de entrega no momento da compra';
comment on column orders.order_date is 'timestamp (utc) que registra o momento exato em que o pedido foi criado';
comment on column orders.paid_at is 'timestamp (utc) que registra o momento exato em que o pagamento do pedido foi confirmado com sucesso';
comment on column orders.billed_at is 'timestamp (utc) que registra o momento exato em que a nota fiscal do pedido foi gerada com sucesso';
comment on column orders.shipped_at is 'timestamp (utc) que registra o momento exato em que o pedido foi despachado para entrega';
comment on column orders.payment_key is 'identificador da transação no gateway de pagamento';
comment on column orders.observations is 'observações sobre o pedido; recebe também o texto vindo do webhook de pagamento';
comment on column orders.status is 'status atual do pedido';
comment on column orders.total is 'valor total do pedido, congelado na criação';
comment on column orders.tracking_code is 'código de rastreamento emitido pela transportadora';
comment on column orders.invoice_id is 'identidade da nota fiscal no serviço billing; o documento é resgatado sob demanda';
comment on column orders.version is 'controle de concorrência otimista (JPA @Version)';


-- Tabela: order_items
create table order_items (
    id bigserial not null,
    order_id bigint not null,
    product_id bigint not null,
    product_name varchar(200) not null,
    amount int not null,
    unit_price decimal(16,2) not null,

    constraint pk_order_items_id primary key (id),
    constraint fk_order_items_orders_id foreign key (order_id) references orders (id),
    constraint chk_order_items_amount check (amount > 0),
    constraint chk_order_items_unit_price check (unit_price >= 0),
    constraint chk_order_items_product_name_not_blank check (btrim(product_name) <> '')
);

comment on column order_items.id is 'chave primária da tabela de itens do pedido';
comment on column order_items.order_id is 'referência ao pedido ao qual este item pertence';
comment on column order_items.product_id is 'referência ao produto comprado';
comment on column order_items.product_name is 'nome do produto no momento da compra; snapshot, não ponteiro para o catálogo';
comment on column order_items.amount is 'quantidade do produto no pedido';
comment on column order_items.unit_price is 'preço unitário do produto no momento da compra';

create index idx_order_items_order_id
    on order_items (order_id);


-- Tabela: payment_info
create table payment_info (
    id bigserial not null,
    order_id bigint not null,
    payment_type varchar(20) not null,
    metadata text,
    created_at timestamp with time zone not null default now(),

    constraint pk_payment_info_id primary key (id),
    constraint fk_payment_info_orders_id foreign key (order_id) references orders (id),
    constraint uq_payment_info_orders_id unique (order_id),
    constraint chk_payment_info_payment_type check (
        payment_type in ('DEBIT', 'CREDIT', 'PAYPAL', 'PIX')
    )
);


-- Tabela: canceled_orders - para auditoria/motivo de cancelamento
create table canceled_orders (
    id bigserial not null,
    order_id bigint not null,
    reason varchar(500),
    cancellation_initiator varchar(30) not null,
    canceled_at timestamp with time zone not null default now(),

    constraint pk_canceled_orders_id primary key (id),
    constraint fk_canceled_orders_orders_id foreign key (order_id) references orders (id),
    constraint uq_canceled_orders_orders_id unique (order_id),
    constraint chk_canceled_orders_initiator check (
        cancellation_initiator in ('CUSTOMER', 'MERCHANT', 'SYSTEM', 'ADMIN')
    ),
    constraint chk_canceled_orders_reason_required check (
        cancellation_initiator = 'CUSTOMER'
        or (
            cancellation_initiator in ('MERCHANT', 'SYSTEM', 'ADMIN')
            and reason is not null
            and btrim(reason) <> ''
        )
    )
);


-- Tabela para OutBox de Orders
create table outbox_messages (
    -- Gerado como UUIDv7 (RFC 9562) na aplicação.
    id uuid not null,

    -- Metadados do evento
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100) not null,
    event_type varchar(100) not null,
    event_version integer not null,
    occurred_at timestamp with time zone not null,
    channel varchar(50) not null,

    -- Chave de particionamento do Kafka, distinta da identidade do agregado (orderId)
    message_key varchar(200),

    -- Rastreamento do fluxo distribuído
    correlation_id varchar(64) not null,
    causation_id varchar(64),

    -- Conteúdo congelado no momento da transação. É o contrato publicado verbatim
    payload jsonb not null,

    -- Estado de processamento da outbox
    status varchar(30) not null default 'PENDING',
    attempts int not null default 0,
    max_attempts int not null default 5,

    -- Idempotência da criação da mensagem na outbox
    idempotency_key varchar(200) not null,

    -- Agendamento e lease do worker.
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
        aggregate_type in ('ORDER')
    ),

    constraint chk_outbox_event_type check (
        event_type in ('PAYMENT_REQUEST_REQUIRED', 'ORDER_PAID', 'ORDER_READY_FOR_SHIPMENT')
        ),

    constraint chk_outbox_event_version check (
        event_version > 0
    ),

    constraint chk_outbox_status check (
        status in ('PENDING', 'PROCESSING', 'PROCESSED', 'FAILED', 'DEAD')
    ),

    constraint chk_outbox_channel check (
        channel in ('PAYMENT', 'EMAIL', 'MESSAGING')
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
            channel in ('PAYMENT', 'EMAIL')
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
        btrim(correlation_id) <> ''
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

comment on table outbox_messages is 'Outbox transacional do orders: eventos e tarefas gravados na mesma transação do agregado e entregues depois por um relay.';
comment on column outbox_messages.id is 'Identidade da linha e eventId publicado no header event-id. UUIDv7, contrato externo.';
comment on column outbox_messages.aggregate_type is 'Tipo do agregado que originou a mensagem. Neste serviço, sempre ORDER.';
comment on column outbox_messages.aggregate_id is 'Identificador do agregado. varchar para acomodar IDs numéricos ou UUIDs.';
comment on column outbox_messages.event_type is 'Tipo do evento ou tarefa produzida pelo orders: PAYMENT_REQUEST_REQUIRED, ORDER_PAID ou ORDER_READY_FOR_SHIPMENT.';
comment on column outbox_messages.event_version is 'Versão do contrato do payload. Existe desde a primeira mensagem para que nenhum consumidor precise tratar ausência como v1.';
comment on column outbox_messages.occurred_at is 'Momento em que o fato ocorreu no domínio, distinto de created_at, que é quando a linha foi gravada.';
comment on column outbox_messages.channel is 'Meio de entrega responsável pela mensagem: PAYMENT, EMAIL ou MESSAGING.';
comment on column outbox_messages.message_key is 'Chave de particionamento do Kafka. Nula onde não há partição a escolher.';
comment on column outbox_messages.correlation_id is 'Identifica o fluxo distribuído inteiro. Será populado a partir do trace-id quando o OpenTelemetry entrar.';
comment on column outbox_messages.causation_id is 'Identifica o evento anterior da cadeia. Permanece nulo quando ausente: inventá-lo registraria linhagem falsa.';
comment on column outbox_messages.payload is 'Conteúdo em JSONB, congelado na transação e publicado verbatim.';
comment on column outbox_messages.status is 'Estado no ciclo de processamento: PENDING, PROCESSING, PROCESSED, FAILED ou DEAD.';
comment on column outbox_messages.attempts is 'Quantidade de tentativas já realizadas.';
comment on column outbox_messages.max_attempts is 'Tentativas permitidas antes de a mensagem virar DEAD.';
comment on column outbox_messages.idempotency_key is 'Impede duplicidade lógica da mesma tarefa ou evento na criação da linha.';
comment on column outbox_messages.next_attempt_at is 'Momento a partir do qual a mensagem pode ser reivindicada. Nulo para status sem próxima tentativa.';
comment on column outbox_messages.locked_until is 'Fim do lease concedido ao worker que reivindicou a mensagem.';
comment on column outbox_messages.lock_token is 'Prova de posse do lease, exigida nas três conclusões (PROCESSED, FAILED, DEAD).';
comment on column outbox_messages.processed_at is 'Momento da entrega bem-sucedida.';
comment on column outbox_messages.failure_reason is 'Motivo da última falha de ENTREGA. Não confundir com observações do pedido.';
comment on column outbox_messages.created_at is 'Momento em que a linha foi gravada na outbox.';
comment on column outbox_messages.updated_at is 'Momento da última atualização da linha.';



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
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),

    -- Lock Otimista
    version bigint not null default 0,

    constraint pk_shipments primary key (id),
    constraint uq_shipments_order_id unique (order_id),

    constraint chk_shipments_status check (
        status in ('PREPARING_SHIPMENT', 'CANCELED', 'SHIPPED')
    ),

    constraint chk_shipments_shipped_data check (
        status <> 'SHIPPED'
        or (
            tracking_code is not null and btrim(tracking_code) <> ''
            and carrier is not null and btrim(carrier) <> ''
            and shipped_at is not null
        )
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

-- Tabela para Outbox de shipping
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
     -- Identidade da linha e, ao mesmo tempo, o eventId publicado no header `event-id`.
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
     -- locked_until expirar ainda concluiria a mensagem de outro. Podendo gerar perda silenciosa num caso, evento duplicado no outro.
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

-- Auditoria por fluxo.
create index idx_outbox_messages_correlation
    on outbox_messages (correlation_id)
    where correlation_id is not null;



















