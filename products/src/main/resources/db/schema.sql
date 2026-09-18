-- DDL do DB market_sphere_products
-- Tabela de produtos
create table products (
    id bigserial not null,
    name varchar(150) not null,
    unit_price decimal(16,2) not null,
    description text not null,
    active boolean not null default true,

    created_at timestamp with time zone not null default now(),
    updated_at timestamp with time zone not null default now(),

    constraint pk_products_id primary key (id)
);

comment on column products.id is 'identificador único do produto';
comment on column products.name is 'nome do produto';
comment on column products.unit_price is 'preço de uma unidade do produto';
comment on column products.description is 'descrição detalhada do produto';
comment on column products.active is 'define se o produto está ativo e disponível';
comment on column products.created_at is 'Momento em que a linha foi gravada';
comment on column products.updated_at is 'Momento da última atualização da linha.';
