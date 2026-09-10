# Customers

Microsserviço responsável pelo cadastro de clientes e dos endereços deles. O perfil e o endereço são recursos separados: um cliente é criado sem endereço, e o endereço entra depois, por endpoints próprios.

## Arquitetura e dependências

O módulo segue uma estrutura orientada a recursos: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `validator` e `client`. O pacote `internal` isola o que existe para consumo entre serviços.

Dependências externas:

- PostgreSQL;
- BrasilAPI por OpenFeign.

O serviço não produz nem consome Kafka. `orders` o consulta de forma síncrona.

## API HTTP

Base path: `/customers`.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/customers` | `201 Created` | cria o cliente sem endereço e retorna `Location` |
| `GET` | `/customers/{customerId}` | `200 OK` | retorna cliente ativo, com o endereço aninhado ou `null` |
| `PUT` | `/customers/{customerId}` | `204 No Content` | atualiza o perfil, sem tocar no endereço |
| `DELETE` | `/customers/{customerId}` | `204 No Content` | exclusão lógica |
| `POST` | `/customers/{customerId}/reactivate` | `204 No Content` | reativa um cliente inativo |
| `POST` | `/customers/{customerId}/addresses` | `201 Created` | cria o endereço; `409` se o cliente já tiver um |
| `GET` | `/customers/{customerId}/addresses/{addressId}` | `200 OK` | retorna o endereço do cliente informado |
| `PUT` | `/customers/{customerId}/addresses` | `204 No Content` | cria ou substitui o endereço do cliente |

Não existe listagem sob `/customers`. Ela vive na API interna, abaixo.

IDs de path precisam ser positivos.

### API interna

Base path: `/customers/internal`. Destinada a operações internas, não ao público.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `GET` | `/customers/internal/{customerId}` | `200 OK` | retorna o cliente esteja ele ativo ou inativo |
| `GET` | `/customers/internal` | `200 OK` | lista paginada de clientes ativos |

A listagem aceita `page` (padrão `0`) e `size` (padrão `20`, máximo `50`), e responde no formato `PagedModel`: `content` mais um bloco `page` com `size`, `number`, `totalElements` e `totalPages`.

### Autenticação

**Nenhum endpoint verifica autenticação no código atual, incluindo os de `/customers/internal`.** Os dois expõem dados pessoais sem credencial: o primeiro devolve clientes inativos, e a listagem devolve nome, CPF, e-mail e telefone de todos os clientes ativos. A proteção está prevista para a etapa de segurança do roadmap e ainda não existe aqui.

## Contratos

### Cliente

| Campo | Regra |
|---|---|
| `fullName` | obrigatório, até 200 caracteres |
| `nationalId` | exatamente 11 dígitos |
| `email` | e-mail válido, até 150 caracteres |
| `phoneNumber` | obrigatório, até 25 caracteres |

O request de cliente não carrega endereço.

Campo desconhecido no corpo resulta em `400`: o serviço usa `fail-on-unknown-properties`, então enviar um atributo fora do contrato é recusado em vez de ignorado.

### Endereço

| Campo | Regra |
|---|---|
| `postalCode` | exatamente 8 dígitos, sem hífen e sem pontuação |
| `street` | obrigatório, até 100 caracteres |
| `houseNumber` | obrigatório, até 10 caracteres |
| `complement` | opcional, até 50 caracteres |
| `neighborhood` | opcional, até 100 caracteres |
| `city` | obrigatório, até 100 caracteres |
| `state` | obrigatório, até 100 caracteres |

`country` não é aceito no request: é gravado como `BR`, e o banco impede outro valor.

Os dados de endereço são de responsabilidade de quem os envia. A BrasilAPI é consultada apenas para verificar que o CEP existe, em `/cep/v2/{cep}`; rua, bairro, cidade e estado não são preenchidos nem conferidos contra a resposta dela.

### Resposta de cliente

`id`, `fullName`, `nationalId`, `email`, `phoneNumber`, `active` e `address`, que é nulo enquanto o cliente não tiver endereço. O objeto de endereço possui `id`, `postalCode`, `street`, `houseNumber`, `complement`, `neighborhood`, `city`, `state` e `country`.

## Exclusão lógica

A entidade `Customer` usa `@SQLDelete` para transformar delete em `UPDATE customers SET active = false`. `@SQLRestriction("active = true")` oculta inativos das consultas JPA comuns.

As queries nativas de reativação e da API interna ignoram esse filtro deliberadamente. `GET /customers/internal/{customerId}` foi criado para que `orders` distinguisse cliente inexistente de cliente inativo, e `orders` não o consome mais: a criação de pedido usa `GET /customers/{customerId}`, e o detalhe do pedido lê os snapshots gravados com ele. O endpoint permanece para operação interna.

A tabela `addresses` não tem exclusão lógica. Inativar um cliente não altera o endereço dele, que continua existindo e volta a aparecer na reativação.

## Erros

O serviço usa RFC 7807 `ProblemDetail`, com `timestamp` como propriedade adicional. Falhas de validação de corpo acrescentam `errors`, com pares `field`/`message`.

| Situação | Status |
|---|---|
| corpo bem formado que falha na validação | `422` |
| JSON malformado | `400` |
| violação em path ou query param | `400` |
| cliente ou endereço inexistente | `404` |
| e-mail ou documento já utilizado, endereço já cadastrado | `409` |
| violação de unicidade detectada pelo banco | `409` |
| CEP inexistente na BrasilAPI | `422` |
| BrasilAPI indisponível | `502` |

Indisponibilidade da BrasilAPI **bloqueia** o cadastro e a alteração de endereço. O cadastro de cliente não depende dela.

## Persistência

Banco esperado: valor de `DB_CUSTOMERS`.

Tabela `customers`: perfil, contato, flag `active` e unicidade de `national_id` e `email`.

Tabela `addresses`: um endereço por cliente, garantido por `uq_addresses_customer_id`, com chave estrangeira para `customers`. `postal_code` é `varchar(8)` e `country` é `varchar(2)` com padrão `BR` e a constraint `chk_addresses_country`.

O módulo usa `ddl-auto=validate`: a aplicação não cria as tabelas, mas recusa a subida se o mapeamento JPA divergir do schema existente. O DDL está em `src/main/resources/db/schema.sql` e replicado no bloco de clientes do schema consolidado em `marketsphere-infra/database/schema.sql`. Nenhum dos dois é executado pela aplicação ao iniciar, então o banco precisa estar criado antes.

## Configuração

| Variável | Finalidade |
|---|---|
| `DB_HOST` | host PostgreSQL |
| `DB_PORT` | porta PostgreSQL |
| `DB_CUSTOMERS` | banco de clientes |
| `DB_USER` | usuário PostgreSQL |
| `DB_PASSWORD` | senha PostgreSQL |
| `SERVER_PORT` | porta HTTP |
| `BRASIL_API_URL` | URL base da BrasilAPI |

Os timeouts do cliente Feign ficam em `spring.cloud.openfeign.client.config.brasilapi`, com 5000 ms para conexão e leitura.

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw -DskipTests clean verify
```

Existe somente o teste de contexto gerado pelo Spring Initializr, e a CI o compila sem executá-lo. Consulte [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
