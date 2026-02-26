package io.github.jvlealc.marketsphere.shipping.service;

import io.github.jvlealc.marketsphere.shipping.subscriber.event.OrderBilledEvent;

/**
 * Responsável por orquestrar a lógica de negócio principal para
 * processar o envio de um pedido
 */
public interface ShippingService {

    /**
     * Processa a lógica de envio de um pedido faturado
     * @param billedEvent O evento contendo os dados de pedido faturado.
     */
    void processShipment(OrderBilledEvent billedEvent);
}
