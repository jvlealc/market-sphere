# Infraestrutura local

Três projetos Docker Compose independentes que fornecem PostgreSQL, Kafka e MinIO para o ambiente de
desenvolvimento. Não há Compose raiz, nem containers dos cinco microsserviços: as aplicações rodam fora do
Docker, por `./mvnw spring-boot:run`.

```text
marketsphere-infra/
├── database/   docker-compose.yml · schema.sql · data/ (volume local)
├── broker/     docker-compose.yml
└── bucket/     docker-compose.yml · docs/ (volume local)
```

Cada diretório precisa do seu próprio `.env`. Os três são ignorados pelo Git, assim como `database/data` e
`bucket/docs`.

## `database` — PostgreSQL

| Item | Valor declarado |
|---|---|
| Imagem | `postgres:17.6` |
| Container | `db_market_sphere` |
| Porta | `${PORT:-5432}` no host → `5432` |
| Volume | `./data` → `/var/lib/postgresql/data` |
| `restart` | `no` |

Variáveis exigidas em `database/.env`:

| Variável | Finalidade |
|---|---|
| `POSTGRES_USER` | superusuário criado na inicialização |
| `POSTGRES_PASSWORD` | senha do superusuário |
| `POSTGRES_MULTIPLE_DATABASE` | declarada no Compose, **sem efeito** — ver abaixo |
| `PORT` | opcional; porta publicada no host, `5432` por padrão |

> `POSTGRES_MULTIPLE_DATABASE` não é reconhecida pela imagem oficial do PostgreSQL. Ela só teria efeito com
> um script de inicialização montado em `/docker-entrypoint-initdb.d`, e nenhum script está montado. **Os
> cinco bancos precisam ser criados manualmente.**

### `schema.sql`

O arquivo consolida o DDL dos cinco bancos. As cinco primeiras instruções são `create database`, seguidas de
cinco blocos de tabelas identificados por comentário:

| Bloco | Banco |
|---|---|
| `-- DDL do DB market_sphere_customers` | `market_sphere_customers` |
| `-- DDL do DB market_sphere_products` | `market_sphere_products` |
| `-- DDL do DB market_sphere_orders` | `market_sphere_orders` |
| `-- DDL do DB market_sphere_shipping` | `market_sphere_shipping` |
| `-- DDL do DB market_sphere_billing` | `market_sphere_billing` |

**Não existe `\connect` entre os blocos.** Executar o arquivo inteiro em uma única conexão cria os cinco
bancos e depois aplica todas as tabelas no banco em que a sessão está conectada. Cada bloco precisa ser
aplicado no banco correspondente, conectando-se a ele.

`orders`, `billing` e `shipping` também mantêm o DDL do próprio banco em `src/main/resources/db/schema.sql`.
Para esses três, prefira o arquivo do módulo — é o que o teste de integração de `orders` e `billing` usa como
referência. `customers` e `products` não têm arquivo próprio: o DDL deles existe apenas aqui.

## `broker` — Kafka, Zookeeper e Kafka UI

| Serviço | Imagem | Porta no host |
|---|---|---|
| `zookeeper` | `confluentinc/cp-zookeeper:${KAFKA_VERSION}` | `22181` → `2181` |
| `kafka` | `confluentinc/cp-kafka:${KAFKA_VERSION}` | `${HOST_KAFKA_PORT}` |
| `kafka-ui` | `provectuslabs/kafka-ui:${KAFKA_UI_VERSION}` | `${HOST_KAFKA_UI_PORT}` → `8080` |

Variáveis exigidas em `broker/.env`:

| Variável | Finalidade |
|---|---|
| `KAFKA_VERSION` | tag das imagens Confluent do broker e do Zookeeper |
| `KAFKA_UI_VERSION` | tag da imagem do Kafka UI |
| `HOST_KAFKA_PORT` | porta do broker publicada no host |
| `HOST_KAFKA_UI_PORT` | porta da interface web |

O broker anuncia dois listeners: `PLAINTEXT://kafka:9092` para quem está na rede do Compose e
`PLAINTEXT_HOST://localhost:${HOST_KAFKA_PORT}` para processos no host. Como as aplicações rodam fora do
Docker, o `KAFKA_BOOTSTRAP_SERVERS` de cada serviço deve apontar para `localhost:${HOST_KAFKA_PORT}`.

A porta do broker é publicada como `${HOST_KAFKA_PORT}:${HOST_KAFKA_PORT}` — o valor escolhido vale dentro e
fora do container. A porta do Zookeeper é fixa no arquivo, não parametrizada.

O Kafka UI é configurado com um cluster chamado `local`. O fator de replicação do tópico de offsets é `1`,
coerente com o broker único.

> **Não há provisionamento dos tópicos de origem.** As aplicações declaram apenas as DLTs dos tópicos que
> consomem — os cinco tópicos principais dependem da configuração do broker ou de criação manual. Antes de
> exercitar o fluxo, confirme no Kafka UI que os valores de `KAFKA_TOPIC_*` são os mesmos no produtor e no
> consumidor de cada evento.

## `bucket` — MinIO

| Item | Valor declarado |
|---|---|
| Imagem | `minio/minio:RELEASE.2025-06-13T11-33-47Z` |
| Container | `minio` |
| API | `${HOST_MINIO_PORT}` → `9000` |
| Console | `${HOST_MINIO_UI_PORT}` → `9001` |
| Volume | `./docs` → `/data` |
| `restart` | `unless-stopped` |

Variáveis exigidas em `bucket/.env`:

| Variável | Finalidade |
|---|---|
| `MINIO_ROOT_USER` | usuário raiz, usado para entrar na console |
| `MINIO_ROOT_PASSWORD` | senha raiz |
| `HOST_MINIO_PORT` | porta da API S3 publicada no host |
| `HOST_MINIO_UI_PORT` | porta da console web |

> **O Compose não cria bucket.** O adaptador de `billing` executa `putObject`, `getObject` e a geração de URL
> pré-assinada, mas nunca `makeBucket`. Crie o bucket pela console antes de faturar o primeiro pedido, e use
> o mesmo nome em `MINIO_BILLING_BUCKET`.

No MinIO, as credenciais raiz também funcionam como par de acesso S3, então elas servem para preencher
`MINIO_ACCESS_KEY` e `MINIO_SECRET_KEY` do `billing`. Criar credenciais de serviço próprias pela console é a
alternativa, e o `billing` não distingue os dois casos.

## Operação

Cada diretório é um projeto independente e sobe por conta própria:

```bash
cd marketsphere-infra/database && docker compose up -d
cd marketsphere-infra/broker   && docker compose up -d
cd marketsphere-infra/bucket   && docker compose up -d
```

Para encerrar, `docker compose down` no diretório correspondente.

Os três não compartilham rede Docker declarada, e não precisam: as aplicações os alcançam pelo host, e o
Kafka UI fala com o broker dentro do próprio projeto do `broker`.

## Dados locais

`database/data` e `bucket/docs` são diretórios do repositório montados dentro dos containers. **`docker
compose down` não os remove** — os bancos e os PDFs de notas anteriores sobrevivem entre execuções.

Isso tem duas consequências práticas:

- recriar os bancos do zero exige apagar `database/data` manualmente, com os containers parados;
- se você apagar os bancos sem apagar os tópicos do Kafka, mensagens de execuções anteriores continuam no
  broker referenciando pedidos que não existem mais. Elas falham no consumo e vão para a DLT. É ruído
  previsível, não defeito.

## O que este diretório não faz

- não sobe os cinco microsserviços, nem contém `Dockerfile` para eles;
- não cria os bancos, nem aplica o `schema.sql`;
- não cria o bucket do MinIO;
- não provisiona os tópicos Kafka;
- não versiona arquivos `.env.example`.

O procedimento manual completo, na ordem, está em [Desenvolvimento local](../docs/local-development.md).
