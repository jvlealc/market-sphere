package io.github.jvlealc.marketsphere.billing.infrastructure.config;

import io.github.jvlealc.marketsphere.billing.application.exception.UnbillableOrderException;
import io.github.jvlealc.marketsphere.billing.domain.exception.InvoiceDomainException;
import io.github.jvlealc.marketsphere.billing.infrastructure.exception.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.billing.infrastructure.config.props.KafkaTopicsProps;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.RetryListener;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.util.backoff.BackOff;

/**
 * Política de erro dos consumidores.
 * <p>
 * Sem um {@code CommonErrorHandler} declarado, vale o {@code DefaultErrorHandler} implícito do Spring:
 * dez tentativas imediatas, sem backoff, e depois <strong>descarte com commit do offset</strong>. O evento
 * some deixando uma linha de log. Toda a classificação de falhas do módulo assume que "propagar" significa
 * "o Kafka reentrega e, esgotado, preserva" — é esta classe que torna isso verdade.
 * <p>
 * Só existe um bean aqui porque o Boot injeta qualquer {@code CommonErrorHandler} do contexto na factory
 * que ele mesmo auto-configura. Se alguém voltar a declarar {@code kafkaListenerContainerFactory} à mão,
 * este handler deixa de ser aplicado — em silêncio.
 */
@Configuration
@Slf4j
public class KafkaErrorHandlingConfig {

    private static final String DLT_SUFFIX = ".DLT";

    private static final int MAX_RETRIES = 4;
    private static final long INITIAL_INTERVAL_MS = 1_000L;
    private static final double MULTIPLIER = 2.0;
    private static final long MAX_INTERVAL_MS = 30_000L;

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<?, ?> kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterRecoverer(kafkaTemplate), backOff());

        // Falhas em que repetir não muda o resultado: vão para a DLT na primeira ocorrência, sem gastar
        // tentativas. As três correspondem à classificação já usada no domínio e na aplicação —
        // payload ilegível, pedido não faturável e invariante violada (bug ou linha corrompida).
        errorHandler.addNotRetryableExceptions(
                MessagingDeserializationException.class,
                UnbillableOrderException.class,
                InvoiceDomainException.class
        );

        errorHandler.setRetryListeners(new DeadLetterRetryListener());

        return errorHandler;
    }

    /**
     * O resolver devolve partição {@code -1} de propósito. O comportamento padrão publica na <em>mesma</em>
     * partição do registro original; se a DLT tiver menos partições que o tópico de origem, a recuperação
     * falha e a mensagem se perde de verdade — justamente no caminho que existe para não perder nada.
     * Com {@code -1}, quem escolhe a partição é o Kafka.
     */
    private static DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<?, ?> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)
        );

        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    /**
     * Quatro retentativas — cerca de 15s no total — antes da DLT. As esperas são bloqueantes: o container
     * pausa a partição durante o backoff. É aceitável neste volume; se um dia não for, o caminho é
     * {@code @RetryableTopic} (retry em tópicos separados, não bloqueante), e não aumentar este intervalo.
     */
    private static BackOff backOff() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(INITIAL_INTERVAL_MS);
        backOff.setMultiplier(MULTIPLIER);
        backOff.setMaxInterval(MAX_INTERVAL_MS);
        return backOff;
    }

    /**
     * Declarada em vez de deixar nascer por {@code auto.create.topics.enable}: auto-criação usa o
     * {@code num.partitions} do broker, varia entre ambientes e não deixa rastro de quem criou o quê.
     * O billing declara o que é dele — a DLT do tópico que ele consome.
     */
    @Bean
    public NewTopic paidOrdersDeadLetterTopic(KafkaTopicsProps topics) {
        return TopicBuilder.name(topics.paidOrders() + DLT_SUFFIX)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Torna visível o que de outro modo se dilui no log do framework. Uma mensagem chegando à DLT é evento
     * de operação, não detalhe técnico: é onde um alerta deve ser pendurado.
     */
    private static final class DeadLetterRetryListener implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception exception, int deliveryAttempt) {
            log.warn("Delivery attempt {} failed for {}-{} offset {}: {}",
                    deliveryAttempt, record.topic(), record.partition(), record.offset(),
                    exception.getMessage());
        }

        @Override
        public void recovered(ConsumerRecord<?, ?> record, Exception exception) {
            log.error("Record from {}-{} offset {} exhausted retries and was sent to {}{}.",
                    record.topic(), record.partition(), record.offset(),
                    record.topic(), DLT_SUFFIX, exception);
        }

        @Override
        public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
            log.error("Could not publish {}-{} offset {} to the dead letter topic. The record will be retried.",
                    record.topic(), record.partition(), record.offset(), failure);
        }
    }
}
