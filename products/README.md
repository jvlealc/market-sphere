# Products

Microsserviço responsável pelo catálogo de produtos, preço e disponibilidade. Produtos são inativados por exclusão lógica e podem ser reativados.

## Arquitetura e dependências

O módulo segue estrutura orientada a recursos: `controller`, `service`, `repository`, `model`, `dto` e tratamento de exceções.

Sua única dependência externa de runtime é PostgreSQL. O serviço não produz nem consome Kafka. `orders` consulta o catálogo por OpenFeign antes de persistir o snapshot dos itens.

## API HTTP

Base path: `/products`.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/products` | `201 Created` | cria produto e retorna `Location` |
| `GET` | `/products/{productId}` | `200 OK` | retorna produto ativo |
| `GET` | `/products` | `200 OK` | lista produtos ativos |
| `GET` | `/products?productsIds=<id>&productsIds=<id>` | `200 OK` | retorna os IDs solicitados, incluindo produtos inativos |
| `DELETE` | `/products/{productId}` | `204 No Content` | exclusão lógica |
| `POST` | `/products/{productId}/reactivate` | `204 No Content` | reativa produto inativo |

Não existe endpoint de atualização de produto. IDs de path precisam ser positivos. Nenhum endpoint verifica autenticação no código atual.

O nome do query parameter é literalmente `productsIds`. Se o parâmetro estiver ausente ou vazio, a API lista os ativos. Se houver IDs, uma query nativa ignora o filtro de exclusão lógica; `orders` usa esse comportamento para detectar produtos inativos.

## Contratos

### Criação

| Campo | Regra |
|---|---|
| `name` | obrigatório, de 1 a 150 caracteres |
| `unitPrice` | obrigatório e maior ou igual a zero |
| `description` | obrigatório, de 5 a 10.000 caracteres |

### Resposta

A representação possui `id`, `name`, `unitPrice`, `description` e `active`.

## Exclusão lógica

A entidade usa `@SQLDelete` para converter delete em `UPDATE products SET active = false`. `@SQLRestriction("active = true")` oculta produtos inativos das consultas JPA comuns.

As queries nativas usadas na reativação e na busca por `productsIds` acessam explicitamente registros inativos.

## Erros

O serviço não usa RFC 7807. `ErrorResponseDto` possui `timestamp`, `status`, `message`, `errors` e `path`; erros de validação usam pares `field`/`error`.

Casos tratados incluem payload ilegível, validação de corpo ou path e produto inexistente.

## Persistência

Banco esperado: valor de `DB_PRODUCTS`.

Tabela `products`:

- `id`;
- `name`;
- `unit_price`;
- `description`;
- `active`.

O módulo usa `ddl-auto=none` e não possui `src/main/resources/db/schema.sql`. O DDL está no bloco de produtos do schema consolidado em `marketsphere-infra/database/schema.sql`.

## Configuração

| Variável | Finalidade |
|---|---|
| `DB_HOST` | host PostgreSQL |
| `DB_PORT` | porta PostgreSQL |
| `DB_PRODUCTS` | banco de produtos |
| `DB_USER` | usuário PostgreSQL |
| `DB_PASSWORD` | senha PostgreSQL |
| `SERVER_PORT` | porta HTTP |

## Executar e testar

```bash
./mvnw spring-boot:run
```

```bash
./mvnw -DskipTests clean verify
```

Existe somente o teste de contexto gerado pelo Spring Initializr, e a CI o compila sem executá-lo. Consulte [Testes e CI](../docs/testing-and-ci.md) e [Desenvolvimento local](../docs/local-development.md).
