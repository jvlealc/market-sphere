# Market Sphere 🌐

Aplicação back-end em arquitetura de microsserviços desenvolvida utilizando **Java** e **Spring Boot**, com foco em comunicação assíncrona orientada a eventos via **Apache Kafka**.

## Estrutura do Projeto

O sistema foi desenhado para atuar como um e-commerce/marketplace descentralizado. A aplicação está dividida em diretórios que representam cada microsserviço com seu respectivo domínio de negócio, além de um diretório dedicado à infraestrutura:

### Microsserviços
- **`billing/`**: Serviço responsável pelas regras de faturamento, pagamentos e transações financeiras.
- **`customers/`**: Serviço dedicado ao cadastro, autenticação e gestão do perfil de clientes.
- **`orders/`**: Serviço central para a criação, orquestração e acompanhamento do status dos pedidos.
- **`products/`**: Serviço que gerencia o catálogo, detalhes e disponibilidade dos produtos.
- **`shipping/`**: Serviço responsável pela logística, cálculo de fretes e rastreio de entregas.

### Infraestrutura
- **`marketsphere-infra/`**: Diretório que contém as configurações e arquivos de orquestração de contêineres (`docker-compose`) para subir os recursos de banco de dados, mensageria e armazenamento necessários para o ecossistema local.

## Tecnologias e Arquitetura

As principais tecnologias e padrões que baseiam o ecossistema deste projeto incluem:

* **[Java](https://www.java.com/)**: Linguagem principal do projeto (100% da base de código).
* **[Spring Boot](https://spring.io/projects/spring-boot)**: Framework base utilizado para a construção e injeção de dependências dos microsserviços.
* **[Apache Kafka](https://kafka.apache.org/)**: Mensageria/Broker de eventos utilizado para garantir a comunicação assíncrona, resiliência e baixo acoplamento entre as APIs.
* **Arquitetura de Microsserviços**: Separação clara de responsabilidades (Domain-Driven) facilitando a manutenção e a escalabilidade independente de cada domínio.

### Contêineres e Serviços Externos (Docker)

O ambiente de desenvolvimento utiliza o **Docker** para fornecer os seguintes serviços de infraestrutura:
* **Banco de Dados**: Instância do **PostgreSQL** para persistência relacional.
* **Armazenamento em Nuvem (Object Storage)**: **MinIO** para simulação de buckets S3 compatíveis, gerenciando arquivos e mídias do sistema.
* **Mensageria**: Ecossistema Confluent contendo **Zookeeper**, o broker do **Kafka** e o **Kafka UI** para monitoramento visual dos tópicos e mensagens.

## Contribuição

Sinta-se à vontade para realizar um *fork* do projeto, abrir *issues* para sugerir melhorias ou enviar *pull requests*.

## Licença

Este projeto é distribuído sob a licença **MIT**. Veja o arquivo [LICENSE](./LICENSE) para mais detalhes.

---
*Copyright (c) 2025 João Leal*