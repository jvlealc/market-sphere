# Desenvolvimento local

## Estado atual do bootstrap

O ambiente local não é inicializado por um comando único. O repositório contém três projetos Docker Compose independentes para PostgreSQL, Kafka e MinIO, mas não contém:

- um Compose raiz que orquestre toda a infraestrutura e os microsserviços;
- imagens ou containers dos cinco aplicativos;
- script funcional para criar os cinco bancos;
- montagem automática dos schemas no container PostgreSQL;
- criação automática do bucket usado por `billing`;
- arquivos `.env.example` versionados.

Este guia descreve esses passos como manuais. Consulte também a [documentação da infraestrutura](../marketsphere-infra/README.md).

## Pré-requisitos

- JDK 21;
- Docker e Docker Compose;
- cliente PostgreSQL ou outra ferramenta capaz de executar o DDL por banco;
- acesso a um servidor SMTP com STARTTLS e autenticação para os serviços que enviam e-mail;
- acesso HTTP à BrasilAPI para criar ou alterar clientes.

O Maven não precisa estar instalado globalmente: cada serviço possui Maven Wrapper.

## Arquivos de configuração

Cada aplicação importa opcionalmente `classpath:.env`. No layout atual, isso corresponde a:

```text
<servico>/src/main/resources/.env
```

Esses arquivos e os `.env` da infraestrutura são ignorados pelo Git. Crie-os localmente sem versionar credenciais. Use sintaxe de properties, uma atribuição por linha:

```dotenv
NOME_DA_VARIAVEL=<valor-local>
```

Os valores dependem do ambiente local. As tabelas abaixo listam somente nomes realmente referenciados pelos arquivos versionados.

### Variáveis comuns das aplicações

| Variável | Usada por | Finalidade |
|---|---|---|
| `DB_HOST` | todos | host PostgreSQL |
| `DB_PORT` | todos | porta PostgreSQL |
| `DB_USER` | todos | usuário PostgreSQL |
| `DB_PASSWORD` | todos | senha PostgreSQL |
| `SERVER_PORT` | todos | porta HTTP do serviço |
| `DB_CUSTOMERS` | `customers` | nome do banco de clientes |
| `DB_PRODUCTS` | `products` | nome do banco de produtos |
| `DB_ORDERS` | `orders` | nome do banco de pedidos |
| `DB_BILLING` | `billing` | nome do banco de faturamento |
| `DB_SHIPPING` | `shipping` | nome do banco de entregas |

Nenhuma porta HTTP padrão é definida nos `application.yml`; `SERVER_PORT` é obrigatória para iniciar cada aplicação.

### Kafka

| Variável | Serviços |
|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `orders`, `billing`, `shipping` |
| `KAFKA_CONSUMER_GROUP_ID` | `orders`, `billing`, `shipping` |
| `KAFKA_TOPIC_PAID_ORDERS` | `orders`, `billing` |
| `KAFKA_TOPIC_BILLED_ORDERS` | `orders`, `billing` |
| `KAFKA_TOPIC_READY_FOR_SHIPMENT_ORDERS` | `orders`, `shipping` |
| `KAFKA_TOPIC_PREPARING_SHIPMENT_ORDERS` | `orders`, `shipping` |
| `KAFKA_TOPIC_SHIPPED_ORDERS` | `orders`, `shipping` |

Use o mesmo valor de tópico no produtor e no consumidor correspondente. Cada serviço deve ter um `KAFKA_CONSUMER_GROUP_ID` apropriado ao seu próprio consumo; o repositório não fixa esses valores.

### SMTP

`orders`, `billing` e `shipping` exigem:

- `SPRING_MAIL_HOST`;
- `SPRING_MAIL_PORT`;
- `SPRING_MAIL_USERNAME`;
- `SPRING_MAIL_PASSWORD`.

As propriedades atuais ativam autenticação e STARTTLS. A indisponibilidade do SMTP afeta os workers de e-mail, não muda os contratos Kafka.

### Integrações específicas

| Variável | Serviço | Finalidade |
|---|---|---|
| `BRASIL_API_URL` | `customers` | URL base da BrasilAPI |
| `ORDERS_SERVICE_API_KEY` | `customers` | segredo esperado no endpoint interno |
| `CUSTOMERS_CLIENT_HOST` | `orders` | host de `customers` |
| `CUSTOMERS_CLIENT_PORT` | `orders` | porta de `customers` |
| `PRODUCTS_CLIENT_HOST` | `orders` | host de `products` |
| `PRODUCTS_CLIENT_PORT` | `orders` | porta de `products` |
| `CUSTOMERS_SERVICE_API_KEY_FOR_ORDERS` | `orders` | segredo enviado a `customers` |
| `MOCK_BANK_WEBHOOK_SECRET` | `orders` | segredo esperado no webhook de pagamento |
| `MINIO_HOST` | `billing` | esquema e host usados no endpoint MinIO |
| `MINIO_PORT` | `billing` | porta da API MinIO |
| `MINIO_ACCESS_KEY` | `billing` | credencial de acesso |
| `MINIO_SECRET_KEY` | `billing` | credencial secreta |
| `MINIO_BILLING_BUCKET` | `billing` | bucket de documentos |

`ORDERS_SERVICE_API_KEY` e `CUSTOMERS_SERVICE_API_KEY_FOR_ORDERS` precisam representar o mesmo segredo. A URL de MinIO é formada por `MINIO_HOST:MINIO_PORT`; `MINIO_HOST` deve incluir o esquema aceito pelo cliente, como definido pelo ambiente do operador.

## Subir a infraestrutura

Crie os `.env` locais de cada diretório com as variáveis descritas no [README da infraestrutura](../marketsphere-infra/README.md). Em terminais separados ou sequencialmente:

```bash
cd marketsphere-infra/database
docker compose up -d
```

```bash
cd marketsphere-infra/broker
docker compose up -d
```

```bash
cd marketsphere-infra/bucket
docker compose up -d
```

Os comandos apenas iniciam os containers. Eles não concluem a preparação descrita nas seções seguintes.

## Preparar os bancos

O Compose usa a imagem oficial `postgres:17.6`. A variável `POSTGRES_MULTIPLE_DATABASE` presente no arquivo não é processada pela imagem sem um script adicional, e nenhum script está montado em `/docker-entrypoint-initdb.d`.

Crie manualmente os bancos:

- `market_sphere_customers`;
- `market_sphere_products`;
- `market_sphere_orders`;
- `market_sphere_shipping`;
- `market_sphere_billing`.

Os nomes acima são os nomes declarados no início de `marketsphere-infra/database/schema.sql`; as variáveis `DB_*` das aplicações precisam apontar para os bancos efetivamente criados.

Depois, conectado individualmente a cada banco:

| Banco | DDL disponível |
|---|---|
| `market_sphere_customers` | bloco `DDL do DB market_sphere_customers` do schema consolidado |
| `market_sphere_products` | bloco `DDL do DB market_sphere_products` do schema consolidado |
| `market_sphere_orders` | `orders/src/main/resources/db/schema.sql` |
| `market_sphere_shipping` | `shipping/src/main/resources/db/schema.sql` |
| `market_sphere_billing` | `billing/src/main/resources/db/schema.sql` |

Não execute `marketsphere-infra/database/schema.sql` inteiro em uma única conexão: o arquivo cria os bancos, mas não usa `\connect` entre os blocos. As tabelas de cada bloco precisam ser aplicadas no banco correspondente.

`customers` e `products` usam `ddl-auto=none`; eles não criam as tabelas ao iniciar. `shipping` usa `ddl-auto=validate`; `orders` e `billing` também não geram o schema da aplicação.

## Preparar o MinIO

O Compose inicia o servidor e a console do MinIO, mas não cria buckets. Entre na console com `MINIO_ROOT_USER` e `MINIO_ROOT_PASSWORD` e crie manualmente o bucket cujo nome será fornecido a `billing` em `MINIO_BILLING_BUCKET`.

O adaptador de `billing` chama `putObject`, `getObject` e gera URL pré-assinada; ele não executa `makeBucket`.

## Kafka

O Compose inicia Zookeeper, um broker Kafka e Kafka UI. Não há script de provisionamento dos cinco tópicos de origem. Os aplicativos declaram apenas as DLTs dos tópicos que consomem, e o Compose não desativa explicitamente a autocrição de tópicos.

Antes de exercitar o fluxo, confirme no Kafka UI que os valores escolhidos para `KAFKA_TOPIC_*` são consistentes entre produtores e consumidores.

## Iniciar os microsserviços

Depois que banco e dependências de um serviço estiverem disponíveis:

```bash
cd <servico>
./mvnw spring-boot:run
```

Ordem recomendada para o fluxo completo:

1. PostgreSQL, Kafka e MinIO;
2. `customers` e `products`;
3. `billing` e `shipping`;
4. `orders`.

A ordem reduz falhas durante o primeiro uso, mas não substitui health checks: o repositório não implementa orquestração dos processos Spring Boot.

## Exercitar o fluxo

Use os contratos dos READMEs de cada serviço. A sequência funcional é:

1. criar cliente e produtos;
2. criar pedido com IDs existentes e ativos;
3. aguardar o worker registrar a chave simulada de pagamento no pedido;
4. enviar ao webhook o `orderId`, a chave registrada, um `webhookEventId`, o resultado e, no sucesso, `paidAt`;
5. acompanhar os eventos de faturamento e preparação no Kafka UI;
6. despachar a entrega por `shipmentId` ou `orderId`;
7. consultar os detalhes do pedido e a URL temporária da nota.

O repositório não expõe endpoint para obter diretamente a chave simulada de pagamento antes de ela aparecer no estado persistido; para um teste manual completo, essa chave precisa ser obtida nos dados ou logs do ambiente local.

## Encerrar

Em cada diretório de infraestrutura:

```bash
docker compose down
```

O PostgreSQL e o MinIO usam diretórios locais montados. `docker compose down` não remove esses diretórios; a remoção dos dados deve ser uma decisão manual e consciente.
