# Billing

Serviço responsável por transformar um pedido pago em nota fiscal PDF, armazenar o documento no MinIO, notificar o cliente e informar a `orders` que o faturamento foi concluído.

## Arquitetura

O módulo usa arquitetura hexagonal com DDD:

- agregado `Invoice` e regras de estado no domínio;
- casos de uso e portas na aplicação;
- adaptadores de Kafka, REST, JPA, JasperReports, MinIO, SMTP e Jackson na infraestrutura.

ArchUnit verifica as fronteiras entre as áreas.

## Dependências e fluxo

Dependências de runtime:

- PostgreSQL;
- Kafka;
- MinIO;
- SMTP.

Ao consumir `ORDER_PAID`, o serviço:

1. encontra ou cria uma nota `PROCESSING` para o `orderId`;
2. gera o PDF com JasperReports;
3. armazena o objeto em `invoices/orders/{orderId}/{invoiceId}.pdf` no bucket configurado;
4. confirma a nota como `GENERATED`;
5. grava duas mensagens `ORDER_BILLED`, uma para Kafka e uma para e-mail.

Falha classificada como pedido não faturável muda a nota para `FAILED`. Outras falhas propagam para a política de retentativa do consumidor. Reprocessar uma nota `GENERATED` ou `FAILED` não gera novo documento.

## API HTTP

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `GET` | `/invoices/{invoiceId}/document` | `307 Temporary Redirect` | redireciona para URL pré-assinada do MinIO |

`invoiceId` é UUID. A URL expira após 15 minutos conforme a configuração atual. Nota inexistente resulta em `404`; documento de nota ainda indisponível resulta em `409`.

O endpoint não verifica autenticação no código atual.

## Estado da nota

| Estado | Significado |
|---|---|
| `PROCESSING` | nota criada, documento ainda não confirmado |
| `GENERATED` | PDF armazenado e metadados de geração persistidos |
| `FAILED` | falha de negócio terminal |

Existe uma nota por pedido (`order_id` único). `storage_key` também é única quando preenchida.

## Outbox e mensageria

| Papel | Evento | Canal |
|---|---|---|
| consome | `ORDER_PAID` | Kafka |
| produz | `ORDER_BILLED` | `MESSAGING` |
| entrega | `ORDER_BILLED` | `EMAIL` |

O payload do canal de e-mail possui a chave do documento e dados do destinatário. O worker recupera o PDF do MinIO e o envia como anexo. Uma indisponibilidade SMTP não bloqueia o worker Kafka porque os canais têm schedulers e configurações distintas.

O consumidor de `ORDER_PAID` tem DLT e backoff exponencial. Veja [Catálogo de eventos](../docs/event-catalog.md) e [Transactional Outbox](../docs/transactional-outbox.md).

## Erros HTTP

O serviço usa RFC 7807 `ProblemDetail`. O handler cobre validação, payload ilegível, recurso inexistente, documento indisponível e falhas internas. Problemas conhecidos recebem `type`, `title`, `status`, `detail` e `instance`; validações acrescentam os campos inválidos.

## Persistência

Banco esperado: valor de `DB_BILLING`. O DDL está em `src/main/resources/db/schema.sql`.

| Tabela | Conteúdo |
|---|---|
| `invoices` | estado e metadados do documento fiscal |
| `outbox_messages` | publicação Kafka e entrega de e-mail |

`Invoice` usa lock otimista. O schema valida a coerência dos campos de `PROCESSING`, `GENERATED` e `FAILED` e a posse por lease das mensagens.

## Configuração

### Banco e HTTP

`DB_HOST`, `DB_PORT`, `DB_BILLING`, `DB_USER`, `DB_PASSWORD` e `SERVER_PORT`.

### Kafka

`KAFKA_BOOTSTRAP_SERVERS`, `KAFKA_CONSUMER_GROUP_ID`, `KAFKA_TOPIC_PAID_ORDERS` e `KAFKA_TOPIC_BILLED_ORDERS`.

### MinIO

| Variável | Finalidade |
|---|---|
| `MINIO_HOST` | esquema e host do endpoint |
| `MINIO_PORT` | porta da API |
| `MINIO_ACCESS_KEY` | chave de acesso |
| `MINIO_SECRET_KEY` | chave secreta |
| `MINIO_BILLING_BUCKET` | bucket existente para documentos |

O serviço não cria o bucket. `MINIO_HOST` e `MINIO_PORT` são concatenados com `:` pela configuração atual.

### SMTP

`SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME` e `SPRING_MAIL_PASSWORD`.

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw clean verify
```

`verify` inclui testes do agregado, da outbox, ArchUnit e integração de repositório com PostgreSQL via Testcontainers. Veja [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
