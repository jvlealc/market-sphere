# Testes e CI

## Projetos independentes

Não existe POM Maven na raiz. Cada um dos cinco serviços possui `pom.xml` e Maven Wrapper próprios, portanto os comandos precisam ser executados dentro do módulo.

```bash
cd <servico>
./mvnw clean verify
```

Os testes de integração de `orders`, `billing` e `shipping` usam Testcontainers e exigem um runtime de contêineres compatível com a API do Docker.

## Matriz da CI

O workflow `.github/workflows/ci.yml` executa:

| Job | Módulos | Comando efetivo |
|---|---|---|
| `verify` | `orders`, `billing`, `shipping` | `./mvnw -B --no-transfer-progress clean verify` |
| `compile` | `customers`, `products` | `./mvnw -B --no-transfer-progress -DskipTests clean verify` |

Os dois jobs usam Amazon Corretto 21, cache Maven e matriz com `fail-fast: false`. Em falhas do job `verify`, relatórios Surefire e Failsafe são publicados como artefato por sete dias.

O workflow reage a pushes e pull requests dirigidos a `main` ou `develop`, além de `workflow_dispatch`.

Apenas o gatilho de `push` filtra caminhos: um push restrito a `*.md`, `LICENSE` ou `.gitignore` não dispara a CI. Pull requests não têm filtro e sempre executam os cinco jobs, mesmo quando alteram somente documentação.

A existência do workflow não comprova, por si só, regras de proteção de branch configuradas no GitHub.

## Cobertura por serviço

### `orders`

- `OrderTest`: criação, pagamento, faturamento, preparação, envio, cancelamento e reidratação do agregado;
- `OutboxMessageTest`: invariantes, tentativas, record key, próxima tentativa e reidratação;
- `OutboxRelayServiceTest`: claim, prazos, conclusões, perda de lease e classificação de falhas;
- `ArchitectureTest`: fronteiras da arquitetura hexagonal;
- `SpringDataOutboxRepositoryIT`: queries nativas de claim/conclusão, `FOR UPDATE SKIP LOCKED` e validação JPA contra PostgreSQL real;
- `OrderLifecycleIT`: o pedido percorre `PAYMENT_PENDING` até `SHIPPED` gravando a cada transição, submetendo a linha aos CHECK de coerência da tabela.

Os dois testes de integração compartilham um único container PostgreSQL, iniciado uma vez por execução e com o schema aplicado na criação. O Failsafe executa classes `*IT` durante `verify`.

### `billing`

- `InvoiceTest`: identidade, criação, geração, falha terminal e reidratação;
- `OutboxMessageTest`: invariantes e ciclo de vida da mensagem;
- `OutboxRelayServiceTest`: comportamento do relay e leases;
- `ArchitectureTest`: fronteiras da arquitetura hexagonal;
- `SpringDataOutboxRepositoryIT`: queries nativas e validação JPA contra PostgreSQL real.

O Failsafe executa classes `*IT` durante `verify`.

### `shipping`

- `ShipmentTest`: criação, identidade, despacho, cancelamento e controle de entrega do e-mail;
- `OutboxMessageTest`: construção da linha de outbox, campos exigidos e estado inicial;
- `OutboxRelayPropsTest`: invariantes de configuração do relay e cálculo do backoff exponencial;
- `OutboxRelayServiceTest`: classificação de falha terminal e retentável, posse perdida e continuidade do lote;
- `OutboxPayloadSerializerTest`: serialização do payload como objeto JSON;
- `OutboxJpaRepositoryIT`: consultas nativas de reivindicação e conclusão, `FOR UPDATE SKIP LOCKED` e validação JPA contra PostgreSQL real.

O Failsafe executa classes `*IT` durante `verify`. Não há teste versionado de consumidor Kafka, publisher, controller ou scheduler no módulo.

### `customers` e `products`

Cada módulo contém apenas o teste `contextLoads` gerado pelo Spring Initializr. O job `compile` usa `-DskipTests`: os fontes de teste são compilados, mas o teste não é executado na CI.

Não existem testes versionados para regras de validação, exclusão lógica, controllers, repositórios ou integração com a BrasilAPI.

## Estratégia observada

Os testes de `Order`, `Invoice`, `Shipment` e das mensagens de outbox são unitários e não iniciam Spring nem banco. Os agregados são conduzidos por suas operações públicas; casos específicos também validam reidratação de estado persistido.

ArchUnit restringe dependências das camadas de aplicação de `orders` e `billing` por listas permitidas. Os testes Testcontainers existem porque as queries da outbox dependem de semântica PostgreSQL que não seria reproduzida de forma fiel por banco em memória.

## Comandos úteis

Executar todos os testes e empacotar um módulo:

```bash
./mvnw clean verify
```

Executar somente os testes unitários Surefire:

```bash
./mvnw test
```

Compilar main e testes sem executá-los, como no job de `customers` e `products`:

```bash
./mvnw -DskipTests clean verify
```

Como não há agregador na raiz, testar o sistema inteiro requer repetir o comando em cada diretório.
