# Market Sphere 🌐

Aplicação back-end em arquitetura de microsserviços desenvolvida utilizando **Java** e **Spring Boot**, com foco em comunicação assíncrona orientada a eventos via **Apache Kafka**.

## 🏗️ Estrutura do Projeto

O sistema foi desenhado para atuar como um e-commerce/marketplace descentralizado. A aplicação está dividida em diretórios que representam cada microsserviço com seu respectivo domínio de negócio:

- **`billing/`**: Serviço responsável pelas regras de faturamento, pagamentos e transações financeiras.
- **`customers/`**: Serviço dedicado ao cadastro, autenticação e gestão do perfil de clientes.
- **`orders/`**: Serviço central para a criação, orquestração e acompanhamento do status dos pedidos.
- **`products/`**: Serviço que gerencia o catálogo, detalhes e disponibilidade dos produtos.
- **`shipping/`**: Serviço responsável pela logística, cálculo de fretes e rastreio de entregas.
- **`marketsphere-services/`**: Disponibiliza dados de infraestrutura e ecossistema da aplicação.

## 💻 Tecnologias e Arquitetura

As principais tecnologias e padrões que baseiam o ecossistema deste projeto incluem:

* **[Java](https://www.java.com/)**: Linguagem principal do projeto (100% da base de código).
* **[Spring Boot](https://spring.io/projects/spring-boot)**: Framework base utilizado para a construção e injeção de dependências dos microsserviços.
* **[Apache Kafka](https://kafka.apache.org/)**: Mensageria/Broker de eventos utilizado para garantir a comunicação assíncrona, resiliência e baixo acoplamento entre as APIs.
* **Arquitetura de Microsserviços**: Separação clara de responsabilidades (Domain-Driven) facilitando a manutenção e a escalabilidade independente de cada domínio.

## 🤝 Contribuição

Sinta-se à vontade para realizar um *fork* do projeto, abrir *issues* para sugerir melhorias ou enviar *pull requests*.

## 📜 Licença

Este projeto é distribuído sob a licença **MIT**. Veja o arquivo [LICENSE](./LICENSE) para mais detalhes.

---
*Copyright (c) 2025 João Leal*