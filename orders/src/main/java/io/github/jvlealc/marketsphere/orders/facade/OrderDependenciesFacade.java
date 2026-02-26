package io.github.jvlealc.marketsphere.orders.facade;

import io.github.jvlealc.marketsphere.orders.client.banking.representation.BankingPaymentRepresentation;
import io.github.jvlealc.marketsphere.orders.client.customers.representation.CustomerRepresentation;
import io.github.jvlealc.marketsphere.orders.client.products.representation.ProductRepresentation;
import io.github.jvlealc.marketsphere.orders.exception.client.customers.CustomerClientNotFoundException;
import io.github.jvlealc.marketsphere.orders.exception.client.products.ProductClientNotFoundException;

import java.util.List;
import java.util.Map;

/**
 * Facade principal e interface de alto nível para o domínio de Pedidos.
 *
 * Centraliza o acesso aos microsserviços de Produtos e Clientes,
 * além da integração com gateway de pagamentos (simulado).
 *
 * Isola o domínio da complexidade de infraestrutura, como chamadas
 * a Feign clients, tratamento de erros de rede e lógica de integração.
 */
public interface OrderDependenciesFacade {

    /**
     * Buscar dados de um cliente pelo seu ID
     *
     * @param customerId O ID do cliente
     * @return {@code CustomerRepresentation}
     * @throws CustomerClientNotFoundException Se o cliente não for encontrado
     * */
    CustomerRepresentation getCustomerById(Long customerId);

     /**
     * Busca os dados de um produto pelo seu ID.
     *
     * @param productId O ID do produto a ser buscado.
     * @return {@code ProductRepresentation}
     * @throws ProductClientNotFoundException Se o produto não for encontrado.
     */
    ProductRepresentation getProductById(Long productId);

    /**
     * Busca os dados de um conjunto de produtos por uma lista de IDs.s
     *
     * @param productIds Uma lista de IDs de produtos a serem buscados.
     * @return {@code Map<Long, ProductRepresentation>} Um <i>Map</i> dos produtos encontrados indexados pelo seu ID
     */
    Map<Long, ProductRepresentation> getProductsByIds(List<Long> productIds);

    /**
     * <strong>(Simulação)</strong> Inicia uma solicitação de pagamento para um pedido específico.
     *
     * @param orderId ID do pedido do pagamento.
     * @return {@code BankingPaymentRepresentation} O resultado da solicitação de pagamento.
     */
    BankingPaymentRepresentation requestPayment(Long orderId);

    /**
     * Busca dados de um cliente pelo seu ID, esteja ele ativo ou inativo.
     *
     * Destinado a cenários internos onde o status de exclusão lógica é irrelevante como o histórico.
     *
     * @param customerId O ID do cliente
     * @return {@code CustomerRepresentation} cliente ativo ou inativo
     * @throws CustomerClientNotFoundException Se o cliente não for encontrado
     */
    CustomerRepresentation getCustomerByIdIgnoringFilter(Long customerId);
}
