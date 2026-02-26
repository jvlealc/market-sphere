package io.github.jvlealc.marketsphere.orders.service;

import io.github.jvlealc.marketsphere.orders.model.Order;
import io.github.jvlealc.marketsphere.orders.subscriber.event.OrderBilledEvent;
import io.github.jvlealc.marketsphere.orders.subscriber.event.OrderShippedEvent;

/**
 * Serviço responsável por gerenciar o ciclo de vida de um Pedido.
 *
 * Esta interface lida especificamente com as transições de estado que
 * são iniciadas por eventos assíncronos como mensagens do Kafka,
 * notificações de Webhook.
 */
public interface OrderLifecycleService {

    /**
     * Processa um evento de pagamento bem-sucedido.
     * Altera o status do pedido para PAID e publica o próximo evento.
     *
     * @param orderId O ID do pedido que foi pago.
     */
    void processSuccessfulPayment(Long orderId);

    /**
     * Processa um evento de falha no pagamento.
     * Altera o status do pedido para PAYMENT_ERROR.
     *
     * @param order O pedido que falhou.
     * @param observations Detalhes da falha.
     */
    void processPaymentError(Order order, String observations);

    /**
     * Processa um evento de pedido faturado vindo do Kafka.
     * Altera o status do pedido para BILLED.
     *
     * @param orderBilledEvent O evento contendo os dados do faturamento.
     */
    void processOrderBilled(OrderBilledEvent orderBilledEvent);

    /**
     * Processa um evento de pedido enviado vindo do Kafka.
     * Altera o status do pedido para SHIPPED.
     *
     * @param orderShippedEvent O evento contendo os dados do envio.
     */
    void processOrderShipped(OrderShippedEvent orderShippedEvent);
}
