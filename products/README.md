# Products

Microsserviço responsável pelo catálogo de produtos, preço e disponibilidade. Produtos são inativados por exclusão lógica e podem ser reativados.

## Arquitetura e dependências

O módulo segue estrutura orientada a recursos: `controller`, `service`, `repository`, `model`, `dto` e `mapper`. Duas exceções à divisão por estereótipo: `internal` reúne o controller, o service e o repositório da API interna, e `shared/rest` guarda o tratamento global de exceções e o envelope de paginação.

Sua única dependência externa de runtime é PostgreSQL. O serviço não produz nem consome Kafka. `orders` consulta o catálogo por OpenFeign antes de persistir o snapshot dos itens.

## API HTTP

Base path: `/products`.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `POST` | `/products` | `201 Created` | cria produto e retorna `Location` |
| `GET` | `/products/{productId}` | `200 OK` | retorna produto ativo |
| `GET` | `/products` | `200 OK` | lista paginada de produtos ativos |
| `DELETE` | `/products/{productId}` | `204 No Content` | exclusão lógica |
| `POST` | `/products/{productId}/reactivate` | `204 No Content` | reativa produto inativo |

Não existe endpoint de atualização de produto. IDs de path precisam ser positivos.

A listagem aceita `pageNumber` (padrão `0`) e é ordenada por `updated_at` decrescente, com `id` crescente como desempate. O tamanho da página é fixo em 20 e não é negociável pelo cliente.

### API interna

Base path: `/internal/products`. Destinada ao consumo por `orders` e a operações internas, não ao público.

| Método | Caminho | Sucesso | Comportamento |
|---|---|---|---|
| `GET` | `/internal/products/{productId}` | `200 OK` | retorna produto ativo |
| `GET` | `/internal/products` | `200 OK` | lista paginada de produtos ativos |
| `GET` | `/internal/products/including-inactives?productsIds=<id>` | `200 OK` | retorna os IDs solicitados, incluindo produtos inativos |

O nome do query parameter é literalmente `productsIds`, ele é obrigatório e aceita de 1 a 50 IDs por requisição. A busca usa query nativa que ignora o filtro de exclusão lógica, e é dela que `orders` depende para distinguir produto inexistente de produto inativo. A resposta é uma lista simples, sem paginação: quem pede um conjunto exato de IDs já sabe quantos itens espera.

### Autenticação

**Nenhum endpoint verifica autenticação no código atual, incluindo os de `/internal/products`.** A separação entre as duas APIs é organizacional, não uma fronteira de segurança: `/internal/products/including-inactives` expõe a qualquer cliente os produtos ocultados pela exclusão lógica. A proteção está prevista para a etapa de segurança do roadmap e ainda não existe aqui.

### Paginação

As duas listagens respondem no mesmo envelope, com todos os campos no primeiro nível: `content`, `pageNumber`, `pageSize`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious` e `empty`. As buscas por ID não usam envelope.

## Contratos

### Criação

| Campo | Regra |
|---|---|
| `name` | obrigatório, de 1 a 150 caracteres |
| `unitPrice` | obrigatório e maior ou igual a zero |
| `description` | obrigatório, de 5 a 10.000 caracteres |

Campo desconhecido no corpo resulta em `400`: o serviço usa `fail-on-unknown-properties`, então enviar um atributo fora do contrato é recusado em vez de ignorado.

### Resposta

A representação possui `id`, `name`, `unitPrice`, `description` e `active`. As colunas de auditoria não são expostas.

## Exclusão lógica

A entidade usa `@SQLDelete` para converter delete em `UPDATE products SET active = false, updated_at = now()`. A coluna de auditoria entra no comando explicitamente porque SQL declarado na anotação não passa pelo `@UpdateTimestamp`. `@SQLRestriction("active = true")` oculta produtos inativos das consultas JPA comuns.

As queries nativas usadas na reativação e em `/internal/products/including-inactives` acessam explicitamente registros inativos.

## Erros

O serviço responde em RFC 7807 `ProblemDetail`, com `timestamp` como propriedade adicional.

Validação do corpo resulta em `422`, com a lista de pares `field`/`message` na propriedade `errors`. Violação em path ou query param resulta em `400`, assim como payload ilegível. Produto inexistente resulta em `404`. Há tratamento de violação de integridade para `409`, mas nenhuma constraint o alcança hoje: a tabela só tem chave primária.

## Persistência

Banco esperado: valor de `DB_PRODUCTS`.

Tabela `products`:

- `id`;
- `name`;
- `unit_price`;
- `description`;
- `active`;
- `created_at` e `updated_at`, ambos `timestamp with time zone` preenchidos pelo banco.

O módulo usa `ddl-auto=validate`: a aplicação não cria as tabelas, mas recusa a subida se o mapeamento JPA divergir do schema existente. O DDL está em `src/main/resources/db/schema.sql` e replicado no bloco de produtos do schema consolidado em `marketsphere-infra/database/schema.sql`. Nenhum dos dois é executado pela aplicação ao iniciar, então o banco precisa estar criado antes.

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
