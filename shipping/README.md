# Shipping

Serviço responsável por criar a preparação de entrega de um pedido faturado, registrar o despacho, propagar o código de rastreio para `orders` e enviar a confirmação por e-mail.

O serviço não consulta uma transportadora nem acompanha movimentações do pacote. O rastreio implementado é o armazenamento e a propagação do código informado no despacho.

## Arquitetura

O módulo usa package by feature:

- `shipment`: agregado, repositórios, serviços, endpoint, consumo e notificação;
- `outbox`: persistência, relay, serialização e publicação;
- `messaging`: envelope e configuração Kafka;
- `rest` e `config`: tratamento comum de HTTP e beans de infraestrutura.

Não há separação hexagonal nem teste ArchUnit neste módulo.

## Dependências

- PostgreSQL;
- Kafka;
- SMTP.

Não há dependência HTTP de `orders`, `customers`, `products` ou `billing`. Os dados do pedido e do cliente chegam em `ORDER_READY_FOR_SHIPMENT`.

## API HTTP

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/shipments/dispatch` | `204 No Content` | despacha por `shipmentId` ou `orderId` |

Request:

| Campo | Regra |
|---|---|
| `shipmentId` | UUID opcional |
| `orderId` | identificador opcional |
| `trackingCode` | texto obrigatório, até 120 caracteres |
| `carrier` | texto obrigatório, até 100 caracteres |
| `shippedAt` | instante opcional, não pode estar no futuro; usa o relógio do serviço quando ausente |

Pelo menos `shipmentId` ou `orderId` precisa ser informado. Se ambos forem enviados, devem identificar a mesma entrega.

O despacho é identificado por `trackingCode` e `carrier`: repetir a mesma dupla é inócuo, mesmo que o instante difira — o que ocorre quando o cliente omite `shippedAt` e o serviço o gera a cada chamada. Divergir no código ou na transportadora viola a transição de estado.

O endpoint não verifica autenticação. Não existem endpoints para criar, consultar, listar ou cancelar entregas.

## Ciclo da entrega

Ao consumir `ORDER_READY_FOR_SHIPMENT`, o serviço verifica se já existe entrega para o pedido. Se não existir, grava:

- `Shipment` em `PREPARING_SHIPMENT`;
- um `ShipmentEvent` de auditoria;
- a mensagem `ORDER_PREPARING_SHIPMENT` na outbox.

No despacho, o agregado muda para `SHIPPED`, recebe transportadora, código e instante, grava nova auditoria e enfileira `ORDER_SHIPPED`.

Estados persistidos: `PREPARING_SHIPMENT`, `SHIPPED` e `CANCELED`. O agregado suporta cancelamento, mas não há endpoint ou consumidor que o exponha no código atual.

## Outbox e mensageria

| Papel | Evento |
|---|---|
| consome | `ORDER_READY_FOR_SHIPMENT` |
| produz | `ORDER_PREPARING_SHIPMENT` |
| produz | `ORDER_SHIPPED` |

A outbox de `shipping` não possui coluna de canal, `locked_until` ou `lock_token`. O fim da posse do worker é persistido em `next_attempt_at`. O backoff de entrega é exponencial e limitado pela configuração.

O consumidor Kafka tem DLT e backoff exponencial. Consulte [Catálogo de eventos](../docs/event-catalog.md) e [Transactional Outbox](../docs/transactional-outbox.md).

## E-mail de despacho

O e-mail não usa outbox. O próprio `Shipment` armazena:

- `shipment_email_sent_at`;
- `shipment_email_attempts`;
- `shipment_email_next_attempt_at`.

Um scheduler consulta entregas de e-mail elegíveis e cada tentativa roda em transação nova. A configuração atual permite seis tentativas com backoff exponencial, atraso inicial de um minuto e teto de 24 horas.

## Erros HTTP

O serviço usa RFC 7807 `ProblemDetail`. Existem handlers gerais de validação e handlers da feature para request inválido, entrega inexistente, agregado inválido e transição ilegal.

## Persistência

Banco esperado: valor de `DB_SHIPPING`. O DDL está em `src/main/resources/db/schema.sql`, e `ddl-auto=validate` confere o mapeamento no boot.

| Tabela | Conteúdo |
|---|---|
| `shipments` | agregado, cliente destinatário, despacho e controle do e-mail |
| `shipment_events` | auditoria das mudanças de estado |
| `outbox_messages` | eventos Kafka |

Há uma entrega por `order_id`. `Shipment` usa lock otimista.

## Configuração

### Banco e HTTP

`DB_HOST`, `DB_PORT`, `DB_SHIPPING`, `DB_USER`, `DB_PASSWORD` e `SERVER_PORT`.

### Kafka

`KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP_ID`, `KAFKA_TOPIC_READY_FOR_SHIPMENT_ORDERS`, `KAFKA_TOPIC_PREPARING_SHIPMENT_ORDERS` e `KAFKA_TOPIC_SHIPPED_ORDERS`.

### SMTP

`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw clean verify
```

O módulo possui testes unitários de `Shipment`; não possui teste versionado de repositório, Kafka, REST ou scheduler. Veja [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
