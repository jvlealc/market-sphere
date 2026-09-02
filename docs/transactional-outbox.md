# Transactional Outbox

## Objetivo e garantia

`orders`, `billing` e `shipping` usam Transactional Outbox para não depender de um dual write entre PostgreSQL e sistemas externos. A mudança do agregado e a linha que representa a futura entrega pertencem à mesma transação de banco. Um relay assíncrono reivindica a linha, entrega o payload persistido e registra o resultado.

A garantia é **at-least-once**. Uma entrega pode ocorrer mais de uma vez se o processo morrer depois que o destino aceitou a mensagem e antes da conclusão da linha. Por isso, eventos carregam identidade e os consumidores verificam o estado antes de aplicar uma transição.

## Mensagens por serviço

| Serviço | Tipo | Canal | Destino |
|---|---|---|---|
| `orders` | `PAYMENT_REQUEST_REQUIRED` | `PAYMENT` | adaptador de pagamento simulado |
| `orders` | `ORDER_PAID` | `MESSAGING` | Kafka |
| `orders` | `ORDER_PAID` | `EMAIL` | SMTP |
| `orders` | `ORDER_READY_FOR_SHIPMENT` | `MESSAGING` | Kafka |
| `billing` | `ORDER_BILLED` | `MESSAGING` | Kafka |
| `billing` | `ORDER_BILLED` | `EMAIL` | SMTP, anexando o PDF recuperado do MinIO |
| `shipping` | `ORDER_PREPARING_SHIPMENT` | sem coluna de canal | Kafka |
| `shipping` | `ORDER_SHIPPED` | sem coluna de canal | Kafka |

Cada par de tipo e canal de `orders` e `billing` possui agendamento e configuração próprios. `shipping` publica somente no Kafka e, portanto, não persiste canal.

## Conteúdo persistido

A linha é criada com:

- identidade UUIDv7, usada como `event-id` nas publicações Kafka;
- tipo e versão do contrato;
- agregado e instante do fato;
- record key quando o destino é Kafka;
- correlação e causa, quando conhecidas;
- payload JSON serializado;
- chave de idempotência da criação;
- estado, tentativas, próxima tentativa e motivo de falha.

O relay publica o JSON persistido **verbatim**. Ele não reconstrói o payload a partir do estado atual do agregado. `message_key` é a chave do `ProducerRecord`, não um header Kafka.

## Criação transacional

Exemplos implementados:

- a colocação de um pedido persiste `Order` e `PAYMENT_REQUEST_REQUIRED` juntos;
- a confirmação de pagamento grava o pedido `PAID` e duas linhas `ORDER_PAID`, uma para Kafka e outra para e-mail;
- a confirmação de geração da nota grava `Invoice` como `GENERATED` e duas linhas `ORDER_BILLED`;
- ao consumir `ORDER_BILLED`, `orders` grava `BILLED` e `ORDER_READY_FOR_SHIPMENT`;
- ao preparar uma entrega, `shipping` grava `Shipment`, seu evento de auditoria e `ORDER_PREPARING_SHIPMENT`;
- o despacho grava `SHIPPED`, o evento de auditoria e `ORDER_SHIPPED`.

A geração do PDF e a escrita no MinIO ocorrem antes da transação que confirma a nota; a outbox torna atômica a confirmação no banco e suas entregas posteriores, não o upload do objeto.

## Posse em `orders` e `billing`

Os dois serviços reivindicam mensagens por `(channel, eventType)` usando `FOR UPDATE SKIP LOCKED`. O claim coloca a linha em `PROCESSING`, define `locked_until` e gera um `lock_token`.

O token precisa ser apresentado ao concluir como `PROCESSED`, `FAILED` ou `DEAD`. Se o update afetar zero linhas, o worker perdeu o lease; outro worker pode ter reivindicado a mensagem. Essa situação é registrada e não é tratada como falha da nova posse.

Antes de iniciar cada entrega do lote, o relay verifica se resta tempo suficiente no lease. A invariante validada na construção da configuração é:

```text
deliveryTimeout < lockDuration
```

O lote é interrompido quando o prazo restante fica menor que `deliveryTimeout`, evitando exigir um lease proporcional ao lote inteiro.

## Posse em `shipping`

`shipping` não tem `locked_until` nem `lock_token`. O claim muda a linha para `PROCESSING` e grava o fim da posse em `next_attempt_at`. Quando esse instante passa, a consulta volta a considerar a linha processável.

Como as mensagens do lote são entregues em série e recebem o prazo no mesmo claim, a configuração valida:

```text
claimDuration > batchSize × deliveryTimeout
```

O código atual pressupõe um worker do relay de `shipping`; a janela temporal ainda permite recuperar linhas abandonadas depois da morte do processo.

## Falhas e retentativas

Estados persistidos:

| Estado | Significado |
|---|---|
| `PENDING` | ainda não entregue |
| `PROCESSING` | reivindicado por um worker |
| `PROCESSED` | entrega concluída |
| `FAILED` | falha retentável; aguarda `next_attempt_at` |
| `DEAD` | falha terminal ou tentativas esgotadas |

`OutboxDeliveryException` representa entrega retentável. `UndeliverableOutboxMessageException` representa falha classificada como terminal e leva a linha diretamente a `DEAD`. Falhas não classificadas são tratadas como retentáveis por padrão.

`orders` e `billing` usam atraso fixo configurado por worker. `shipping` calcula backoff exponencial limitado por `retry-max-delay`. O número máximo de tentativas é persistido na própria mensagem.

## Consumo Kafka e DLT

Os consumidores dos três serviços usam `DefaultErrorHandler`, `DeadLetterPublishingRecoverer` e quatro retentativas com backoff exponencial. Exceções que indicam payload ilegível ou violação determinística são não retentáveis e seguem para DLT na primeira falha.

O recoverer escolhe a DLT `<topico-origem>.DLT` com partição `-1`, deixando o Kafka selecionar a partição. Cada serviço declara apenas as DLTs dos tópicos que consome.

## E-mail de `shipping`

O e-mail de confirmação de despacho não passa pela outbox. O agregado `Shipment` armazena instante de envio, tentativas e próxima tentativa. Uma consulta ao repositório funciona como fila, e o scheduler processa cada envio em transação nova.

Essa escolha é específica ao e-mail de `shipping`: o conteúdo pode ser reconstruído do agregado e não precisa preservar ordem com eventos Kafka. Os e-mails de `orders` e `billing` continuam isolados em canais de outbox.

## Linhagem

`correlation-id` identifica o fluxo distribuído. `causation-id` identifica o estímulo direto quando conhecido.

- fluxos iniciados em `orders` recebem correlação UUIDv7;
- ao consumir Kafka, o evento produzido a seguir preserva `correlation-id` e usa o `event-id` consumido como causa;
- o primeiro `ORDER_PAID` usa o `webhookEventId` externo como causa; esse valor é texto de até 64 caracteres, não necessariamente UUID;
- `ORDER_SHIPPED` nasce de um comando HTTP e preserva a correlação do `Shipment`, mas atualmente grava causa nula.

O contrato completo do envelope e dos payloads está no [catálogo de eventos](event-catalog.md).
