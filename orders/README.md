# Orders

Serviço central do ciclo de vida comercial do pedido. Ele valida clientes e produtos, persiste snapshots, solicita pagamento por um adaptador simulado, recebe a confirmação por webhook e acompanha faturamento e entrega por eventos Kafka.

## Arquitetura

O módulo usa arquitetura hexagonal com DDD:

- `domain`: agregados, objetos de valor, enums e exceções sem dependência da infraestrutura;
- `application`: casos de uso, comandos, queries, modelos e portas;
- `infrastructure`: REST, Kafka, schedulers, OpenFeign, SMTP, JPA e serialização.

ArchUnit verifica as dependências permitidas entre essas áreas.

## Dependências

- PostgreSQL;
- Kafka;
- `customers` e `products` por OpenFeign;
- SMTP;
- nenhum serviço bancário externo: `MockPaymentClientAdapter` produz a resposta simulada em processo.

## API HTTP

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/orders` | `201 Created` | valida cliente/produtos, cria pedido e retorna `Location` |
| `GET` | `/orders/{orderId}` | `200 OK` | resumo do pedido |
| `GET` | `/orders/{orderId}/details` | `200 OK` | detalhes, snapshots, itens, nota e rastreio |
| `POST` | `/webhooks/payments` | `204 No Content` | registra sucesso ou falha do pagamento |

IDs precisam ser positivos. Os endpoints de pedido não verificam autenticação.

### Criar pedido

| Campo | Regra |
|---|---|
| `customerId` | obrigatório e positivo |
| `paymentInfo` | obrigatório |
| `paymentInfo.metadata` | texto obrigatório |
| `paymentInfo.paymentType` | `DEBIT`, `CREDIT`, `PAYPAL` ou `PIX` |
| `orderItems` | lista não vazia |
| `orderItems[].productId` | obrigatório e positivo |
| `orderItems[].amount` | obrigatório e positivo |

Antes de abrir a transação, o caso de uso consulta o cliente ativo e busca todos os produtos por ID, incluindo inativos. Cliente inativo, produto ausente ou produto inativo impede a criação. Nome e preço dos produtos e os dados do cliente são congelados como snapshots do pedido.

### Resumo e detalhes

O resumo retorna `id`, `customerId`, `orderDate`, `observations`, `status`, `total` e `amountItems`.

Os detalhes retornam `orderId`, snapshot de `customer`, datas de cada etapa, `orderTotal`, `orderStatus`, `orderObservations`, `invoiceId`, `trackingCode` e `orderItems`. Cada item possui `productId`, `productName`, `amount`, `unitPrice` e a disponibilidade atual consultada em `products`.

### Webhook de pagamento

Header obrigatório para autenticação:

```http
X-Webhook-Secret: <segredo-compartilhado>
```

| Campo | Regra |
|---|---|
| `orderId` | obrigatório e positivo |
| `paymentKey` | obrigatório e precisa corresponder ao pedido |
| `webhookEventId` | obrigatório; causa do primeiro evento `ORDER_PAID` |
| `successful` | obrigatório |
| `observations` | opcional; usado em falha |
| `paidAt` | obrigatório quando `successful=true` |

Segredo ausente ou divergente resulta em `401 Unauthorized`. A chave simulada de pagamento é persistida depois que o worker `PAYMENT_REQUEST_REQUIRED` processa o pedido, mas não é exposta nas respostas HTTP atuais.

## Estado do pedido

Estados persistidos:

- `PAYMENT_PENDING`;
- `PAYMENT_ERROR`;
- `PAID`;
- `BILLED`;
- `PREPARING_SHIPMENT`;
- `SHIPPED`;
- `CANCELED`.

Criação e transições repetidas são protegidas por invariantes de domínio. O agregado suporta cancelamento antes do envio, mas a API atual não expõe endpoint de cancelamento.

## Outbox e mensageria

| Papel | Evento/tarefa | Canal |
|---|---|---|
| produz | `PAYMENT_REQUEST_REQUIRED` | `PAYMENT` |
| produz | `ORDER_PAID` | `MESSAGING` e `EMAIL` |
| produz | `ORDER_READY_FOR_SHIPMENT` | `MESSAGING` |
| consome | `ORDER_BILLED` | Kafka |
| consome | `ORDER_PREPARING_SHIPMENT` | Kafka |
| consome | `ORDER_SHIPPED` | Kafka |

Os eventos Kafka usam `orderId` como record key. Os consumidores têm DLT, backoff exponencial e guardas de estado. Consulte [Catálogo de eventos](../docs/event-catalog.md) e [Transactional Outbox](../docs/transactional-outbox.md).

## Erros

As respostas tratadas usam RFC 7807 `ProblemDetail`. O handler diferencia, entre outros casos:

- validação de corpo e parâmetro;
- pedido, item, cliente ou produto ausente;
- cliente/produto inativo;
- conflito de estado;
- falha de serviço externo;
- segredo inválido no webhook.

Falhas internas de domínio, aplicação ou infraestrutura classificadas como inesperadas são registradas e retornam problema `500` sem expor a causa completa.

## Persistência

Banco esperado: valor de `DB_ORDERS`. O schema versionado está em `src/main/resources/db/schema.sql`.

| Tabela | Conteúdo |
|---|---|
| `orders` | agregado, snapshot do cliente, estado e marcos temporais |
| `order_items` | snapshots dos produtos e quantidades |
| `payment_info` | metadados e tipo de pagamento |
| `canceled_orders` | informação de cancelamento |
| `outbox_messages` | tarefas e eventos pendentes ou concluídos |

O agregado usa versão para lock otimista. As chaves estrangeiras existem somente entre tabelas do próprio banco de `orders`.

## Configuração

### Banco e HTTP

`DB_HOST`, `DB_PORT`, `DB_ORDERS`, `DB_USER`, `DB_PASSWORD` e `SERVER_PORT`.

### Clientes HTTP e segredos

| Variável | Finalidade |
|---|---|
| `PRODUCTS_CLIENT_HOST` | host de `products` |
| `PRODUCTS_CLIENT_PORT` | porta de `products` |
| `CUSTOMERS_CLIENT_HOST` | host de `customers` |
| `CUSTOMERS_CLIENT_PORT` | porta de `customers` |
| `CUSTOMERS_SERVICE_API_KEY_FOR_ORDERS` | segredo enviado ao endpoint interno de `customers` |
| `MOCK_BANK_WEBHOOK_SECRET` | segredo esperado no webhook |

### Kafka

`KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP_ID`, `KAFKA_TOPIC_PAID_ORDERS`, `KAFKA_TOPIC_BILLED_ORDERS`, `KAFKA_TOPIC_READY_FOR_SHIPMENT_ORDERS`, `KAFKA_TOPIC_PREPARING_SHIPMENT_ORDERS` e `KAFKA_TOPIC_SHIPPED_ORDERS`.

### SMTP

`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw clean verify
```

`verify` inclui testes unitários, ArchUnit e integração da outbox com PostgreSQL via Testcontainers. Veja [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
