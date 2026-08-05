# Market Sphere 🌐

Aplicação back-end em arquitetura de microsserviços desenvolvida utilizando **Java** e **Spring Boot**, com foco em comunicação assíncrona orientada a eventos via **Apache Kafka**.

## Estrutura do Projeto

O sistema foi desenhado para atuar como um e-commerce/marketplace descentralizado. A aplicação está dividida em diretórios que representam cada microsserviço com seu respectivo domínio de negócio, além de um diretório dedicado à infraestrutura:

### Microsserviços
- **`billing/`**: Serviço responsável pelas regras de faturamento, geração e envio da nota fiscal para email de clientes.
- **`customers/`**: Serviço dedicado ao cadastro, autenticação e gestão do perfil de clientes.
- **`orders/`**: Serviço central para a criação, orquestração e acompanhamento do status dos pedidos.
- **`products/`**: Serviço que realiza cadastro, gerencia o catálogo, e disponibilidade dos produtos.
- **`shipping/`**: Serviço responsável pela logística, envio e rastreio de entregas.

### Infraestrutura
- **`marketsphere-infra/`**: Diretório que contém as configurações e arquivos de orquestração de contêineres (`docker-compose`) para subir os recursos de banco de dados, mensageria e armazenamento necessários para o ecossistema local.

## Tecnologias e Arquitetura

As principais tecnologias e padrões que baseiam o ecossistema deste projeto incluem:

* **[Java](https://www.java.com/)**: Linguagem principal do projeto (100% da base de código).
* **[Spring Boot](https://spring.io/projects/spring-boot)**: Framework base utilizado para a construção e injeção de dependências dos microsserviços.
* **[Apache Kafka](https://kafka.apache.org/)**: Mensageria/Broker de eventos utilizado para garantir a comunicação assíncrona, resiliência e baixo acoplamento entre as APIs.
* **Arquitetura de Microsserviços**: Separação clara de responsabilidades (Domain-Driven) facilitando a manutenção e a escalabilidade independente de cada domínio.
* **Jasper Reports**: Utilizado para elaboração e geração de notas fiscais do sistema.

## Outbox Pattern

`orders` e `billing` publicam eventos pelo **Outbox Pattern**, que elimina o dual-write: gravar no banco e publicar no broker deixam de ser duas operações que podem divergir. A implementação de referência é a do `billing` — o `orders` está sendo convergido para ela.

### Modelagem

Cada operação assíncrona pendente é uma `OutboxMessage` imutável, classificada por três dimensões:

- **`OutboxEventType`**: o que aconteceu (`ORDER_BILLED` no `billing`; `ORDER_PAID` e `PAYMENT_REQUEST_REQUIRED` no `orders`).
- **`OutboxChannel`**: o **meio** de entrega (`MESSAGING`, `EMAIL`, `PAYMENT`) — não a tecnologia. É `MESSAGING`, não `KAFKA`: trocar JavaMail por SendGrid não muda o nome do canal.
- **`OutboxStatus`**: `PENDING` → `PROCESSING` → `PROCESSED`, ou `FAILED` → `DEAD` ao esgotar as tentativas.

Canal separado por meio dá **isolamento de falha por meio**: um SMTP fora do ar não prende as mensagens que iriam para o Kafka, porque cada worker reivindica só o seu par `(canal, tipo)`.

Além do estado de processamento, a linha carrega o **envelope do evento** — `event_version`, `occurred_at`, `message_key`, `correlation_id`, `causation_id` —, que viaja como *headers* do Kafka. Os nomes são próprios (`event-id`, `event-type`, `aggregate-id`), mas mapeáveis 1:1 para o CloudEvents, de modo que adotar o SDK depois seja tabela de renomeação e não redesenho.

### Atomicidade na origem

A `OutboxMessage` nasce na mesma transação da mudança de estado que a originou. No `billing`, `InvoiceGenerationOutcomeService.confirmGeneration` promove a nota a `GENERATED` e enfileira `ORDER_BILLED` nos canais `MESSAGING` e `EMAIL` — três escritas, um commit. Cada mensagem tem sua própria `idempotencyKey`, porque a mesma nota legitimamente produz um evento e um e-mail.

**O payload é congelado na transação e publicado verbatim.** Ele *é* o contrato: desserializar e re-serializar na publicação faria uma mudança de código alterar o conteúdo de linhas gravadas antes dela, anulando a garantia que a outbox existe para dar. Por isso o metadado vai em header, e não no corpo.

> O `orders` ainda faz o oposto — grava um payload minimalista e remonta o evento na publicação, lendo `customers` e `products` naquele instante. Isso produz nota fiscal com o endereço de **hoje** para uma compra de ontem, e acopla a publicação de um fato já consumado à disponibilidade de dois serviços. É o próximo item da fila de refatoração.

### Reivindicação com lease e token de posse

`claimProcessableMessages(channel, eventType, limit, lockDuration)` reivindica um lote sob um *lease* temporal, via `UPDATE ... RETURNING` sobre uma CTE com `FOR UPDATE SKIP LOCKED` — cada instância leva um lote disjunto sem bloquear as demais.

O lease vem acompanhado de um `lock_token` gerado no mesmo `UPDATE`, e as **três** conclusões (`markAsProcessed`, `recordFailure`, `markAsDead`) o exigem. A guarda `status = 'PROCESSING'` sozinha impede que um worker atrasado altere linha já concluída, mas não que ele interfira enquanto outro *ainda está publicando*: nessa janela, sem token, ele marcaria `PROCESSED` algo que não saiu, ou devolveria a `FAILED` uma linha já publicada.

Zero linhas afetadas significa **"perdi o lease"**, não erro: a porta devolve `boolean`, o worker registra e segue. Tratar isso como exceção transformaria a operação mais rotineira de um sistema com lease em alarme de infraestrutura.

| Relay | Canal | Lote | Lease | Timeout de entrega | Retry |
|---|---|---|---|---|---|
| `ProcessOrderBilledMessagingUseCase` | `MESSAGING` | 20 | 60s | 25s | 10s |
| `ProcessOrderBilledEmailUseCase` | `EMAIL` | 10 | 2m30s | 30s | 1m |

Os quatro parâmetros moram juntos em `OutboxRelaySettings` e a invariante `deliveryTimeout < lockDuration` é verificada **no boot**: YAML incoerente derruba a aplicação em vez de produzir evento duplicado em produção. Como o lease é concedido ao lote inteiro mas as mensagens saem em série, o relay ainda interrompe o lote quando o que resta do lease não cobre mais uma entrega inteira — o lote é botão de vazão, não de correção.

### Classificação de falha

O contrato de falha é declarado na **porta**, não no adaptador: `OutboxDeliveryException` (retentável) e `UndeliverableOutboxMessageException` (terminal, vai direto a `DEAD` sem gastar tentativas). Quem captura um tipo concreto de adaptador não tem como saber se cobriu todos os modos de falha dele — e foi assim que, numa versão anterior, toda falha de envio de e-mail escapava sem registrar tentativa, deixando a linha presa em `PROCESSING`.

O default é **retentável**: só se marca como terminal o que se sabe classificar. O relay ainda isola cada mensagem num `catch` de segurança, porque uma exceção escapando do laço prenderia a linha em `PROCESSING` *e* abortaria o resto do lote.

No lado do consumo, um `DefaultErrorHandler` com `DeadLetterPublishingRecoverer` e backoff exponencial garante que nada seja descartado em silêncio — o padrão do Spring são dez tentativas imediatas seguidas de commit do offset.

## Contêineres e Serviços Externos (Docker)

O ambiente de desenvolvimento utiliza o **Docker** para fornecer os seguintes serviços de infraestrutura:
* **Banco de Dados**: Instância do **PostgreSQL** para persistência relacional.
* **Armazenamento de Arquivos (Object Storage)**: **MinIO** para simulação de cloud buckets, gerenciando arquivos (notas fiscais geradas pelo microsserviço de faturamento - `billing`) do sistema.
* **Mensageria**: Ecossistema Confluent contendo **Zookeeper**, o broker do **Kafka** e o **Kafka UI** para monitoramento visual dos tópicos e mensagens.

## Contribuição

Sinta-se à vontade para realizar um *fork* do projeto, abrir *issues* para sugerir melhorias ou enviar *pull requests*.

## Licença

Este projeto é distribuído sob a licença **MIT**. Veja o arquivo [LICENSE](./LICENSE) para mais detalhes.

---
*Copyright (c) 2025 João Leal*