--- Bancos de dados
create database market_sphere_products;
create database market_sphere_orders;
create database market_sphere_customers;
create database market_sphere_shipping;



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
	order_date timestamp with time zone not null default now(),
	paid_at timestamp with time zone null,
	billed_at timestamp with time zone null,
	shipped_at timestamp with time zone null,
	payment_key text,
	observations varchar(255),
	status varchar(30),
	total decimal(16,2) not null,
	tracking_code uuid,
	invoice_url text,

	constraint pk_orders_id primary key (id),
	constraint chk_orders_status check (
        status in ('PAYMENT_PENDING', 'PAID', 'BILLED', 'PREPARING_SHIPMENT', 'SHIPPED', 'PAYMENT_ERROR', 'CANCELED')
    )
);

comment on table orders is 'armazena os pedidos dos clientes';
comment on column orders.id is 'chave primária da tabela de pedidos';
comment on column orders.customer_id is 'referência ao cliente que realizou o pedido';
comment on column orders.order_date is 'timestamp (utc) que registra o momento exato em que o pedido foi criado.';
comment on column orders.paid_at is 'timestamp (utc) que registra o momento exato em que o pagamento do pedido foi confirmado com sucesso.';
comment on column orders.billed_at is 'timestamp (utc) que registra o momento exato em que a nota fiscal do pedido foi gerada com sucesso.';
comment on column orders.shipped_at is 'timestamp (utc) que registra o momento exato em que o pedido foi despachado para entrega.';
comment on column orders.payment_key is 'identificador da transação de pagamento';
comment on column orders.observations is 'observações ou comentários adicionais sobre o pedido';
comment on column orders.status is 'status atual do pedido';
comment on column orders.total is 'valor total do pedido';
comment on column orders.tracking_code is 'código de rastreamento do envio';
comment on column orders.invoice_url is 'link url para a nota fiscal';


-- Tabela: order_items
create table order_items (
	id bigserial not null,
	order_id bigint not null ,
	product_id bigint not null,
	amount int not null,
	unit_price decimal(16,2) not null,

	constraint pk_order_items_id primary key (id),
	constraint fk_order_items_orders_id foreign key (order_id) references orders (id),
	constraint chk_order_items_amount check (amount > 0),
	constraint chk_order_items_unit_price check (unit_price >= 0)
);

comment on column order_items.id is 'chave primária da tabela de itens do pedido';
comment on column order_items.order_id is 'referência ao pedido ao qual este item pertence';
comment on column order_items.product_id is 'referência ao produto comprado';
comment on column order_items.amount is 'quantidade do produto no pedido';
comment on column order_items.unit_price is 'preço unitário do produto no momento da compra';


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
    )
);


-- Tabela para OutBox
create table outbox_messages (
     id uuid not null,
     aggregate_type varchar(100) not null,
     aggregate_id varchar(100) not null,
     event_type varchar(100) not null,
     channel varchar(50) not null,
     payload jsonb not null,

     status varchar(30) not null default 'PENDING',
     attempts int not null default 0,
     max_attempts int not null default 5,

     idempotency_key varchar(200) not null,

     next_attempt_at timestamp with time zone,
     locked_until timestamp with time zone,
     processed_at timestamp with time zone,

     error_message text,

     created_at timestamp with time zone not null default now(),
     updated_at timestamp with time zone not null default now(),

     constraint pk_outbox_messages primary key (id),
     constraint uq_outbox_idempotency_key unique (idempotency_key),

     constraint chk_outbox_event_type check (
         event_type in ('PAYMENT_REQUEST_REQUIRED' 'ORDER_PAID', 'ORDER_BILLED', 'ORDER_SHIPPED')
     )

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
     )
);

create index idx_outbox_messages_pending
    on outbox_messages (channel, status, next_attempt_at, created_at);

create index idx_outbox_messages_aggregate
    on outbox_messages (aggregate_type, aggregate_id);

comment on table outbox_messages is 'Tabela de outbox transacional usada para registrar eventos e tarefas a serem processados de forma assíncrona e confiável.';
comment on column outbox_messages.id is 'Identificador único da mensagem da outbox. Pode ser usado como eventId, correlationId ou referência em logs.';
comment on column outbox_messages.aggregate_type is 'Tipo do agregado relacionado à mensagem. Exemplo: ORDER, PAYMENT, INVOICE ou SHIPMENT.';
comment on column outbox_messages.aggregate_id is 'Identificador do agregado relacionado. Mantido como varchar para permitir IDs numéricos, UUIDs ou outros formatos.';
comment on column outbox_messages.event_type is 'Tipo do evento ou tarefa a ser processada. Exemplo: PAYMENT_REQUEST_REQUIRED, EMAIL_PAYMENT_REQUESTED ou ORDER_PAID.';
comment on column outbox_messages.channel is 'Canal responsável pelo processamento da mensagem. Exemplo: PAYMENT, EMAIL ou KAFKA.';
comment on column outbox_messages.payload is 'Conteúdo da mensagem em JSONB. A estrutura varia conforme o event_type e o channel.';
comment on column outbox_messages.status is 'Estado atual da mensagem no ciclo de processamento da outbox. Valores permitidos: PENDING, PROCESSING, PROCESSED, FAILED ou DEAD.';
comment on column outbox_messages.attempts is 'Quantidade de tentativas já realizadas para processar a mensagem.';
comment on column outbox_messages.max_attempts is 'Quantidade máxima de tentativas permitidas antes de marcar a mensagem como DEAD.';
comment on column outbox_messages.idempotency_key is 'Chave única de idempotência usada para impedir duplicidade lógica da mesma tarefa ou evento.';
comment on column outbox_messages.next_attempt_at is 'Momento a partir do qual a mensagem pode ser processada ou reprocessada. Usado para retry com backoff.';
comment on column outbox_messages.locked_until is 'Momento até o qual a mensagem está reservada por um worker ou processador, evitando processamento simultâneo.';
comment on column outbox_messages.processed_at is 'Momento em que a mensagem foi processada com sucesso.';
comment on column outbox_messages.error_message is 'Última mensagem de erro registrada durante o processamento da mensagem.';
comment on column outbox_messages.created_at is 'Data e hora de criação da mensagem na outbox.';
comment on column outbox_messages.updated_at is 'Data e hora da última atualização da mensagem na outbox.';



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
    customer_email varchar(150) not null,
    customer_name varchar(200) not null,
    shipment_email_sent_at timestamp with time zone,
    created_at timestamp with time zone default now(),
    updated_at timestamp with time zone default now(),

    constraint pk_shipments primary key (id),
    constraint uq_shipments_order_id unique (order_id),

    constraint chk_shipments_status check (
        status in ('PREPARING_SHIPMENT', 'CANCELED', 'SHIPPED')
    ),

    constraint chk_shipments_shipped_date check (
        status <> 'SHIPPED'
        or (
            tracking_code is not null and tracking_code <> ''
            and carrier is not null and carrier <> ''
            and shipped_at is not null
        )
    ),

    constraint chk_shipments_canceled_date check (
        status <> 'CANCELED' or canceled_at is not null
    )
);

create index idx_shipments_status on shipments (status);


create table shipment_events (
    id bigserial not null,
    shipment_id uuid not null,
    shipment_status varchar(30) not null,
    description text,
    occurred_at timestamp with time zone default now(),

    constraint pk_shipment_events primary key (id),
    constraint fk_shipment_events_shipment_id foreign key (shipment_id) references shipments(id) on delete cascade,

    constraint chk_shipment_events_shipment_status check (
        shipment_status in ('PREPARING_SHIPMENT', 'CANCELED', 'SHIPPED')
    )
);

create index idx_shipment_events_shipment_id on shipment_events (shipment_id);









