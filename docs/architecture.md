# Arquitetura

## Visão geral

Market Sphere separa clientes, produtos, pedidos, faturamento e entregas em cinco processos Spring Boot e cinco bancos PostgreSQL. Cada serviço é um projeto Maven independente e não há biblioteca de domínio compartilhada.

```mermaid
flowchart TB
    subgraph Entrada HTTP
        Client[Cliente]
        PaymentProvider[Provedor de pagamento]
        Operator[Operador logístico]
    end

    subgraph Microsserviços
        Customers[customers]
        Products[products]
        Orders[orders]
        Billing[billing]
        Shipping[shipping]
    end

    Client --> Customers
    Client --> Products
    Client --> Orders
    Client --> Billing
    PaymentProvider -->|webhook| Orders
    Operator -->|dispatch| Shipping

    Orders -->|OpenFeign| Customers
    Orders -->|OpenFeign| Products
    Customers -->|OpenFeign| BrasilAPI[BrasilAPI]

    Orders <-->|Kafka| Billing
    Orders <-->|Kafka| Shipping

    Customers --> CDB[(customers DB)]
    Products --> PDB[(products DB)]
    Orders --> ODB[(orders DB)]
    Billing --> BDB[(billing DB)]
    Shipping --> SDB[(shipping DB)]
    Billing --> MinIO[(MinIO)]
    Orders --> SMTP[SMTP]
    Billing --> SMTP
    Shipping --> SMTP
```

O "provedor de pagamento" do diagrama representa a borda do webhook. A solicitação de pagamento não chama um microsserviço bancário: o adaptador atual de `orders` gera uma resposta simulada em processo.

## Limites de domínio

| Domínio | Dado de autoridade | Integrações |
|---|---|---|
| `customers` | perfil, endereço e estado ativo do cliente | consulta CEP na BrasilAPI; serve snapshot a `orders` |
| `products` | nome, descrição, preço e disponibilidade | serve snapshots a `orders` |
| `orders` | pedido, itens, pagamento e estado comercial | consulta clientes/produtos; coordena eventos de faturamento e entrega |
| `billing` | nota fiscal e localização do PDF | consome pedido pago; usa JasperReports, MinIO e SMTP |
| `shipping` | preparação, despacho, transportadora e código de rastreio | consome pedido pronto; publica mudanças logísticas e envia e-mail |

Os bancos não possuem relacionamentos entre serviços. `orders` persiste snapshots do cliente e dos produtos necessários ao pedido. `billing` e `shipping` recebem seus snapshots nos eventos, evitando consultas síncronas durante o processamento Kafka.

## Estilos internos

| Serviço | Estilo | Estrutura observada |
|---|---|---|
| `orders` | Hexagonal + DDD | domínio sem dependência de infraestrutura, casos de uso, portas e adaptadores |
| `billing` | Hexagonal + DDD | agregado `Invoice`, casos de uso e adaptadores de Kafka, JPA, Jasper, MinIO e e-mail |
| `shipping` | Package by feature | `shipment`, `outbox`, `messaging`, `rest` e configuração sem camadas globais |
| `customers` | Orientado a recursos | controller, service, repository, model, mapper e client |
| `products` | Orientado a recursos | controller, service, repository, model e DTOs |

ArchUnit verifica as fronteiras hexagonais de `orders` e `billing`. Não há regra equivalente em `shipping`, `customers` ou `products`.

## Comunicação

### HTTP síncrono

- `orders` consulta `customers` e `products` por OpenFeign antes de persistir um pedido;
- `customers` consulta `/cep/v1/{cep}` da BrasilAPI durante criação ou alteração;
- o endpoint interno de `customers` exige `X-Internal-Service-Auth` e permite a `orders` consultar cliente ativo ou inativo;
- o webhook de `orders` exige `X-Webhook-Secret`;
- `shipping` recebe o comando de despacho por HTTP;
- `billing` redireciona a consulta do documento para uma URL temporária do MinIO.

Não existe Spring Security nem autenticação comum para os endpoints públicos.

### Kafka assíncrono

`orders`, `billing` e `shipping` trocam cinco eventos JSON. Os nomes dos tópicos são configuráveis, e `orderId` é sempre a record key para preservar a ordem por pedido. A entrega é at-least-once; os consumidores tornam reentregas inócuas verificando o estado do agregado ou sua existência.

Consulte o [catálogo de eventos](event-catalog.md) e os detalhes do [Transactional Outbox](transactional-outbox.md).

## Ciclos de vida

### Pedido

```mermaid
stateDiagram-v2
    [*] --> PAYMENT_PENDING
    PAYMENT_PENDING --> PAYMENT_ERROR: falha de pagamento
    PAYMENT_ERROR --> PAYMENT_PENDING: nova chave de pagamento registrada
    PAYMENT_PENDING --> PAID: webhook de sucesso
    PAID --> BILLED: ORDER_BILLED
    BILLED --> PREPARING_SHIPMENT: ORDER_PREPARING_SHIPMENT
    PREPARING_SHIPMENT --> SHIPPED: ORDER_SHIPPED
    PAYMENT_PENDING --> CANCELED
    PAYMENT_ERROR --> CANCELED
    PAID --> CANCELED
    BILLED --> CANCELED
    PREPARING_SHIPMENT --> CANCELED
```

Não há transição direta de `PAYMENT_ERROR` para `PAID`. A confirmação só é aceita a partir de `PAYMENT_PENDING`; registrar uma nova chave de pagamento é o que devolve o pedido a esse estado e torna a confirmação possível.

O código de domínio impede cancelar um pedido já enviado. A API atual não expõe endpoint de cancelamento; a transição existe no agregado.

### Nota fiscal e entrega

- `Invoice`: `PROCESSING` → `GENERATED` ou `FAILED`; `FAILED` é terminal.
- `Shipment`: nasce em `PREPARING_SHIPMENT` e pode ir para `SHIPPED` ou `CANCELED`. A API atual expõe somente o despacho para `SHIPPED`.

## Consistência e falhas

- Mudanças de estado e intenções de entrega são gravadas atomicamente na outbox.
- O relay publica depois do commit; uma falha de processo pode causar reentrega, nunca uma garantia exactly-once.
- Consumidores Kafka usam backoff exponencial e DLT para falhas esgotadas ou classificadas como não retentáveis.
- `orders` e `billing` isolam Kafka, e-mail e pagamento por canal de outbox; `shipping` mantém o e-mail no próprio agregado.

## Erros HTTP

O formato não é uniforme em todo o sistema:

- `orders`, `billing` e `shipping` usam RFC 7807 `ProblemDetail`;
- `customers` e `products` retornam seus próprios `ErrorResponseDto`;
- respostas de autenticação também variam: o endpoint interno de `customers` devolve `403`, enquanto segredo inválido no webhook de `orders` resulta em `401` com `ProblemDetail`.

Os contratos HTTP detalhados ficam nos READMEs de cada serviço.
