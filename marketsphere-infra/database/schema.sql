--- Bancos de dados
create database market_sphere_products;
create database market_sphere_orders;
create database market_sphere_customers;

-- Tabela customers
create table customers (
    id serial not null,
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

comment on table customers is 'Customers master data, including embedded Address value object';

comment on column customers.id is 'Primary key (surrogate)';
comment on column customers.full_name is 'Customer''s full legal name';
comment on column customers.national_id is 'Customer''s national identifier (e.g., CPF in Brazil)';
comment on column customers.email is 'Customer''s unique email address';
comment on column customers.phone_number is 'Customer''s phone number (may include country code)';

-- Address (embedded VO)
comment on column customers.postal_code is 'Customer''s postal code (ZIP, CEP, etc.)';
comment on column customers.street is 'Customer''s street name';
comment on column customers.house_number is 'Customer''s house or building number';
comment on column customers.complement is 'Additional address info (apartment, suite, etc.)';
comment on column customers.neighborhood is 'Customer''s neighborhood or district';
comment on column customers.city is 'Customer''s city';
comment on column customers.state is 'Customer''s state, province or region';
comment on column customers.country is 'Customer''s country';

-- Tabela: products
create table products (
    id serial not null,
    name varchar(150) not null,
    unit_price decimal(16,2) not null,
    description text not null,
    active boolean not null default true,

    constraint pk_products_id primary key (id)
);

comment on column products.id is 'Unique identifier for the product';
comment on column products.name is 'Name of the product';
comment on column products.unit_price is 'Price of a single unit of the product';
comment on column products.description is 'Detailed description of the product';
comment on column products.active is 'Define if product is active (available)';


-- Tabela: orders
create table orders (
	id bigserial not null,
	customer_id bigint not null,
	order_date timestamp with time zone not null default now(),
	paid_at timestamp with time zone null,
	billed_at timestamp with time zone null,
	shipped_at timestamp with time zone null,
	payment_key text,
	observations text,
	status varchar(30),
	total decimal(16,2) not null,
	tracking_code uuid,
	invoice_url text,

	constraint pk_orders_id primary key (id),
	constraint chk_orders_status check (
        status in ('PENDING', 'PLACED', 'PAID', 'BILLED', 'SHIPPED', 'PAYMENT_ERROR', 'PREPARING_SHIPMENT', 'CANCELED')
    )
);

comment on table orders is 'stores customer orders';
comment on column orders.id is 'primary key for orders';
comment on column orders.customer_id is 'reference to the customer who placed the order';
comment on column orders.order_date is 'timestamp (utc) recording the exact moment the order was created.';
comment on column orders.paid_at is 'timestamp (utc) recording the exact moment the order payment was successfully confirmed.';
comment on column orders.billed_at is 'timestamp (utc) recording the exact moment the order invoice was successfully generated.';
comment on column orders.shipped_at is 'timestamp (utc) recording the exact moment the order was dispatched for delivery (shipped).';
comment on column orders.payment_key is 'identifier for the payment transaction';
comment on column orders.observations is 'additional notes or comments about the order';
comment on column orders.status is 'current status of the order';
comment on column orders.total is 'total value of the order';
comment on column orders.tracking_code is 'shipping tracking code';
comment on column orders.invoice_url is 'url link to the invoice';
comment on table orders is 'Stores customer orders';
comment on column orders.id is 'Primary key for orders';
comment on column orders.customer_id is 'Reference to the customer who placed the order';
comment on column orders.order_date is 'Timestamp when the order was created';
comment on column orders.payment_key is 'Identifier for the payment transaction';
comment on column orders.observations is 'Additional notes or comments about the order';
comment on column orders.status is 'Current status of the order';
comment on column orders.total is 'Total value of the order';
comment on column orders.tracking_code is 'Shipping tracking code';
comment on column orders.invoice_url is 'URL link to the invoice';


-- Tabela: order_items
create table order_items (
	id bigserial not null,
	order_id bigint not null ,
	product_id bigint not null,
	amount int not null,
	unit_price decimal(16,2) not null,

	constraint pk_order_items_id primary key (id),
	constraint fk_order_items_order_id foreign key (order_id) references orders (id),
	constraint chk_order_items_amount check (amount > 0),
	constraint chk_order_items_unit_price check (unit_price >= 0)
);

comment on table order_items is 'Stores items of each order';
comment on column order_items.id is 'Primary key for order items';
comment on column order_items.order_id is 'Reference to the order this item belongs to';
comment on column order_items.product_id is 'Reference to the purchased product';
comment on column order_items.amount is 'Quantity of the product in the order';
comment on column order_items.unit_price is 'Unit price of the product at purchase time';
