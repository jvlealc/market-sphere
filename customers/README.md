# Customers

Microsserviço responsável pelo cadastro de clientes. Ele persiste o perfil e o estado ativo do cliente e completa o endereço consultando a BrasilAPI a partir do CEP informado.

## Arquitetura e dependências

O módulo segue uma estrutura orientada a recursos: `controller`, `service`, `repository`, `model`, `dto`, `mapper`, `validator` e `client`.

Dependências externas:

- PostgreSQL;
- BrasilAPI por OpenFeign.

O serviço não produz nem consome Kafka. `orders` o consulta de forma síncrona.

## API HTTP

Base path: `/customers`.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/customers` | `201 Created` | valida CEP na BrasilAPI, cria cliente e retorna `Location` |
| `GET` | `/customers/{customerId}` | `200 OK` | retorna cliente ativo |
| `GET` | `/customers` | `200 OK` | lista clientes ativos e envia `X-Total-Count` |
| `PUT` | `/customers/{customerId}` | `204 No Content` | atualiza perfil e resolve novamente o CEP |
| `DELETE` | `/customers/{customerId}` | `204 No Content` | exclusão lógica |
| `POST` | `/customers/{customerId}/reactivate` | `204 No Content` | reativa um cliente inativo |
| `GET` | `/customers/for-orders-service/{customerId}` | `200 OK` | retorna cliente ativo ou inativo para `orders` |

IDs de path precisam ser positivos.

### Autenticação

Somente `/customers/for-orders-service/{customerId}` verifica autenticação. O cliente precisa enviar:

```http
X-Internal-Service-Auth: <segredo-compartilhado>
```

O valor é comparado com `ORDERS_SERVICE_API_KEY`. Divergência resulta em `403 Forbidden` sem corpo. Os demais endpoints não têm autenticação no código atual.

## Contratos

### Criação e atualização

| Campo | Regra |
|---|---|
| `fullName` | obrigatório, até 200 caracteres |
| `nationalId` | exatamente 11 dígitos |
| `email` | e-mail válido, até 150 caracteres |
| `phoneNumber` | obrigatório, até 25 caracteres |
| `postalCode` | CEP com oito dígitos, com ou sem hífen |
| `number` | obrigatório, até 10 caracteres |
| `complement` | opcional, até 50 caracteres |
| `country` | obrigatório, até 100 caracteres |

Rua, bairro, cidade e estado não chegam no request: são preenchidos pela resposta da BrasilAPI.

### Resposta de cliente

A representação possui `id`, `fullName`, `nationalId`, `email`, `phoneNumber`, `postalCode`, `street`, `houseNumber`, `complement`, `neighborhood`, `city`, `state`, `country` e `active`.

## Exclusão lógica

A entidade usa `@SQLDelete` para transformar delete em `UPDATE customers SET active = false`. `@SQLRestriction("active = true")` oculta inativos das consultas JPA comuns.

As queries nativas de reativação e do endpoint interno ignoram esse filtro deliberadamente. O endpoint interno existe para que `orders` consiga distinguir cliente inexistente de cliente inativo antes de criar um pedido.

## Erros

O serviço não usa RFC 7807. Erros tratados usam `ErrorResponseDto` com:

- `timestamp`;
- `status`;
- `message`;
- `errors`, contendo pares `field`/`error` em falhas de validação;
- `path`.

Casos tratados incluem payload inválido, cliente inexistente, e-mail ou documento já utilizado e falha da BrasilAPI.

## Persistência

Banco esperado: valor de `DB_CUSTOMERS`.

Tabela `customers`:

- perfil e contato;
- endereço embutido;
- flag `active`;
- unicidade de `national_id` e `email`.

O módulo usa `ddl-auto=none` e não possui `src/main/resources/db/schema.sql`. O DDL está no bloco de clientes do schema consolidado em `marketsphere-infra/database/schema.sql`.

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
| `ORDERS_SERVICE_API_KEY` | segredo do endpoint interno |

O `application.yml` atual possui `spring.application` vazio e `spring.name: customers`; portanto, ele não define `spring.application.name` da mesma forma que os outros serviços.

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw -DskipTests clean verify
```

Existe somente o teste de contexto gerado pelo Spring Initializr, e a CI o compila sem executá-lo. Consulte [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
