-- DDL do DB market_sphere_customers
-- Tabela de clientes
create table customers (
    id bigserial not null,
    full_name varchar(200) not null,
    national_id varchar(11) not null,
    email varchar(150) not null,
    phone_number varchar(25) not null,
    active boolean not null default true,

    constraint pk_customers_id primary key (id),
    constraint uq_customers_national_id unique (national_id),
    constraint uq_customers_email unique (email)
);

comment on table customers is 'dados dos clientes';
comment on column customers.id is 'chave primária substituta';
comment on column customers.full_name is 'nome legal completo do cliente';
comment on column customers.national_id is 'identificador nacional do cliente, como CPF no Brasil';
comment on column customers.email is 'endereço de e-mail único do cliente';
comment on column customers.phone_number is 'número de telefone do cliente, podendo incluir código do país';


-- Tabela de endereços de clientes
create table addresses (
    id bigserial not null,
    postal_code varchar(8) not null,
    street varchar(100) not null,
    house_number varchar(10) not null,
    complement varchar(50),
    neighborhood varchar(100),
    city varchar(100) not null,
    state varchar(100) not null,
    country varchar(2) not null default 'BR',
    customer_id bigint not null,

    constraint pk_addresses_id primary key (id),
    constraint chk_addresses_country check (country = 'BR'),
    constraint fk_addresses_customer_id foreign key (customer_id) references customers(id),
    constraint uq_addresses_customer_id unique (customer_id)
);

comment on table addresses is 'dados inerentes ao endereço do cliente';
comment on column addresses.postal_code is 'CEP com exatamente 8 dígitos, sem hífen';
comment on column addresses.street is 'nome da rua';
comment on column addresses.house_number is 'número da casa, prédio ou imóvel';
comment on column addresses.complement is 'informações adicionais do endereço, como apartamento, sala , edifício etc.';
comment on column addresses.neighborhood is 'bairro ou distrito';
comment on column addresses.city is 'cidade';
comment on column addresses.state is 'estado, província ou região';
comment on column addresses.country is 'país no formato ISO 3166-1 alpha-2; restrito a BR';
comment on column addresses.customer_id is 'chave estrangeira para o cliente';
