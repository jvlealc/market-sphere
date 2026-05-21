package io.github.jvlealc.marketsphere.orders.application.ports.out.product;

import java.util.List;
import java.util.Map;

public interface ProductGatewayPort {

    /**
     * Busca um produto pelo ID. Garante que ele existe.
     * Lança ProductNotFoundException se não for encontrado (404 ou 200+null).
     *
     * @param productId O ID do produto a ser buscado.
     * @return A representação do produto.
     */
    ProductSnapshot getProductById(Long productId);

    /**
     * Busca uma lista de produtos por IDs.
     * Retorna um Mapa<ID, Produto> de forma segura.
     * Trata 200 OK + body nulo, retornando um mapa vazio se nenhum produto for encontrado.
     *
     * @param productIds A lista de IDs de produtos a serem buscados.
     * @return Um Mapa onde a chave é o ID do produto e o valor é a representação.
     */
    Map<Long, ProductSnapshot> getProductsByIdsIncludingInactive(List<Long> productIds);
}
