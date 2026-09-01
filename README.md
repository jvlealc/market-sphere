# Market Sphere 🌐

Aplicação back-end em arquitetura de microsserviços desenvolvida utilizando **Java** e **Spring Boot**, com foco em comunicação assíncrona orientada a eventos via **Apache Kafka**.

## Estrutura do Projeto

O sistema foi desenhado para atuar como um e-commerce/marketplace descentralizado. A aplicação está dividida em diretórios que representam cada microsserviço com seu respectivo domínio de negócio, além de um diretório dedicado à infraestrutura:

### Microsserviços

- **`billing/`**: regras de faturamento, geração da nota fiscal e envio por e-mail ao cliente.
- **`customers/`**: cadastro e gestão do perfil de clientes, com validação de endereço via BrasilAPI.
- **`orders/`**: serviço central do ciclo de vida do pedido — criação, pagamento e acompanhamento de status.
- **`products/`**: cadastro e catálogo de produtos.
- **`shipping/`**: logística, despacho e rastreio de entregas.

### Infraestrutura

- **`marketsphere-infra/`**: configurações e arquivos de orquestração de contêineres (`docker-compose`) para subir banco de dados, mensageria e armazenamento do ambiente local, mais o `schema.sql` consolidado dos cinco bancos.

## Tecnologias

* **[Java 21](https://www.java.com/)** e **[Spring Boot 3.5.6](https://spring.io/projects/spring-boot)**.
* **[Apache Kafka](https://kafka.apache.org/)**: comunicação assíncrona entre `orders`, `billing` e `shipping`.
* **PostgreSQL**, um banco por serviço, sem chave estrangeira entre domínios.
* **[MinIO](https://min.io/)**: object storage para as notas fiscais geradas pelo `billing`.
* **[JasperReports](https://community.jaspersoft.com/)**: geração do PDF da nota fiscal.
* **OpenFeign**: comunicação síncrona de (`orders` com `customers` e `products`).
* **MapStruct** e **Lombok**, **ArchUnit**, **Testcontainers**, **GitHub Actions**.

## Arquitetura

> **Estrutura interna varia com a complexidade do domínio.**

Este é o princípio que organiza o repositório, e por isso os cinco serviços **não** seguem o mesmo estilo:

| Serviço | Estilo | Motivação                                                                   |
|---|---|-----------------------------------------------------------------------------|
| `orders`, `billing` | Hexagonal + DDD | Domínios com maior complexidade, regras de negócio e invariantes de domínio |
| `shipping` | Package by feature | Complexidade intermediária, sem camadas artificiais                         |
| `customers`, `products` | Arquitetura simples orientada a recursos | Domínios predominantemente cadastrais (CRUD)                                |

O que **não** varia, porque é protocolo do sistema e não estilo: contrato de evento, política de erro no consumidor, propagação de correlação, formato de erro HTTP (RFC 7807 `ProblemDetail`) e autenticação.

## Transactional Outbox

`orders`, `billing` e `shipping` publicam eventos pelo **Transactional Outbox Pattern**, evitando o problema de dual write ao persistir a mudança de estado e a intenção de publicação na mesma transação de banco.

### O que é comum aos três

**A linha nasce na mesma transação da mudança de estado que a originou.** No `billing`, `confirmGeneration` promove a nota a `GENERATED` e enfileira `ORDER_BILLED` — três escritas, um commit. No `shipping`, o despacho grava `SHIPPED` e a linha do `ORDER_SHIPPED` juntos.

**O payload é serializado e congelado na transação, e publicado verbatim.** O relay publica esse conteúdo sem reconstruí-lo a partir do modelo atual, evitando que mudanças posteriores de serialização alterem eventos já persistidos.

**A linha carrega o envelope do evento** — `event_version`, `occurred_at`, `message_key`, `correlation_id`, `causation_id` —, que viaja como headers do Kafka.

**A chave de partição é o `orderId`**, não a identidade do agregado: é por pedido que os eventos precisam se ordenar do lado de quem consome.

**O contrato de falha é declarado onde a decisão é tomada**, não no adaptador: `OutboxDeliveryException` (retentável) e `UndeliverableOutboxMessageException` (terminal, vai direto a `DEAD` sem gastar tentativas). O default é **retentável**: só se marca como terminal o que se sabe classificar.

### Onde os três divergem — e por quê

| | `orders` / `billing` | `shipping` |
|---|---|---|
| Canais | `MESSAGING`, `EMAIL`, `PAYMENT`, um worker por par `(canal, tipo)` | nenhum — só Kafka |
| Posse | lease temporal + `lock_token` nas três conclusões | prazo em `next_attempt_at`, sem colunas de lock |
| E-mail | pela outbox, em canal próprio | fora da outbox |

O **canal** nomeia o *meio* de entrega, não a tecnologia — é `MESSAGING`, não `KAFKA`, porque trocar JavaMail por SendGrid não muda o nome de `EMAIL`. Ele existe para dar **isolamento de falha por meio**: um SMTP fora do ar não prende as mensagens que iriam para o Kafka.

No `shipping` esse isolamento vem de graça, sem coluna: o e-mail não passa pela outbox. Ele é um efeito só, com conteúdo derivável do agregado e sem ordenação a preservar, então um marcador no próprio `Shipment` basta e **a consulta é a fila** — com backoff de horas a dias, contra segundos do relay de eventos, porque o destinatário é uma pessoa que tem o site enquanto isso.

O **lease com `lock_token`** protege a janela em que um worker ainda está publicando: sem ele, um worker atrasado marcaria `PROCESSED` algo que não saiu, ou devolveria a `FAILED` uma linha já publicada. Zero linhas afetadas significa *"perdi a posse"*, não erro — a operação devolve `boolean`, o worker registra e segue. Com um worker só, o `shipping` obtém o mesmo efeito gravando o prazo da reivindicação em `next_attempt_at`: passado ele, a linha volta a ser reivindicável pela própria consulta de claim, sem varredura separada. A garantia continua sendo **at-least-once**, e é por isso que todo consumidor tem guarda idempotente por estado.

### Os parâmetros do relay moram juntos e são validados no boot

Lote, timeout de entrega, prazo de posse e backoff são números que só fazem sentido juntos. Reunidos num `@ConfigurationProperties`, a relação entre eles é verificada na construção do bean:

```
deliveryTimeout < lockDuration        (orders, billing)
claimDuration   > batchSize x deliveryTimeout   (shipping)
```

A segunda existe porque o prazo é concedido ao **lote inteiro** no instante do claim, mas as mensagens são entregues **em série**: o relógio da última começa a correr quando a primeira foi reivindicada. YAML incoerente derruba a aplicação no boot, em vez de produzir evento duplicado em produção.

### No lado do consumo

`DefaultErrorHandler` com `DeadLetterPublishingRecoverer` e backoff exponencial nos três serviços que consomem Kafka. Falhas em que repetir não muda o resultado (payload ilegível, invariante violada) vão à DLT na primeira ocorrência, sem gastar tentativas.

A DLT é publicada com partição `-1`, deixando o Kafka escolher: o padrão do recoverer usa a mesma partição do original, e se a DLT tiver menos partições a recuperação falha justamente no caminho que existe para não perder nada.

### Linhagem

Cada evento carrega `correlation_id` (o fluxo) e `causation_id` (o evento anterior). A causa direta é sempre o `event-id` da mensagem consumida. Como os IDs são UUIDv7, ordenados no tempo:

```
ORDER_PAID
    ↓
ORDER_BILLED
    ↓
ORDER_READY_FOR_SHIPMENT
    ↓
ORDER_PREPARING_SHIPMENT
    ↓
ORDER_SHIPPED
```

## Qualidade e CI

O pipeline do GitHub Actions compila os cinco microsserviços e executa
as suítes de testes dos módulos aplicáveis. O `billing` inclui um teste
de integração com Testcontainers e PostgreSQL.

A branch `main` é protegida por pull requests e status checks obrigatórios.

## Contêineres e Serviços Externos (Docker)

O ambiente de desenvolvimento utiliza o **Docker** para fornecer os seguintes serviços de infraestrutura:

* **Banco de Dados**: instância do **PostgreSQL** para persistência relacional.
* **Armazenamento de Arquivos**: **MinIO** para simulação de cloud buckets, gerenciando as notas fiscais geradas pelo `billing`.
* **Mensageria**: ecossistema Confluent contendo **Zookeeper**, o broker do **Kafka** e o **Kafka UI** para monitoramento visual dos tópicos e mensagens.

## Contribuição

Sinta-se à vontade para realizar um *fork* do projeto, abrir *issues* para sugerir melhorias ou enviar *pull requests*.

## Licença

Este projeto é distribuído sob a licença **MIT**. Veja o arquivo [LICENSE](./LICENSE) para mais detalhes.

---
*Copyright (c) 2025 João Leal*
