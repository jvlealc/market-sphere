package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import io.github.jvlealc.marketsphere.orders.domain.exception.OrderDomainException;
import io.github.jvlealc.marketsphere.orders.infrastructure.adapters.in.messaging.kafka.MessagingDeserializationException;
import io.github.jvlealc.marketsphere.orders.infrastructure.config.props.KafkaTopicsProps;
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
 * Políticas de erros do Kafka
 * <p>
 * Configura retry e dead-letter handling dos consumidores Kafka.
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
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(deadLetterRecoverer(kafkaTemplate), backOff());

        // Falhas em que repetir não muda o resultado vão para a DLT na primeira ocorrência, sem gastar
        // tentativas. As três correspondem à classificação já usada no domínio e na aplicação —
        // payload ilegível e invariante violada (bug ou linha corrompida).
        errorHandler.addNotRetryableExceptions(
                MessagingDeserializationException.class,
                OrderDomainException.class
        );

        errorHandler.setRetryListeners(new DeadLetterRetryListener());

        return errorHandler;
    }

    // Declaração da DLT de cada tópico consumido por orders //
    @Bean
    public NewTopic billedOrdersDeadLetterTopic(KafkaTopicsProps topics) {
        return TopicBuilder.name(topics.billedOrders() + DLT_SUFFIX)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic preparingShipmentOrdersDeadLetterTopic(KafkaTopicsProps topics) {
        return TopicBuilder.name(topics.preparingShipmentOrders() + DLT_SUFFIX)
                .partitions(1)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic shippedOrdersDeadLetterTopic(KafkaTopicsProps topics) {
        return TopicBuilder.name(topics.shippedOrders() + DLT_SUFFIX)
                .partitions(1)
                .replicas(1)
                .build();
    }

    /**
     * Usa partição {@code -1} para deixar o Kafka escolher a partição da DLT,
     * evitando depender da mesma quantidade de partições do tópico de origem.
     */
    private static DeadLetterPublishingRecoverer deadLetterRecoverer(KafkaTemplate<String, String> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, -1)
        );

        recoverer.setFailIfSendResultIsError(true);

        return recoverer;
    }

    /**
     * Configura quatro retentativas com backoff exponencial. Cerca de 15s no total antes da DLT.
     * As esperas são bloqueantes: o container pausa a partição durante o backoff.
     */
    private static BackOff backOff() {
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(MAX_RETRIES);
        backOff.setInitialInterval(INITIAL_INTERVAL_MS);
        backOff.setMultiplier(MULTIPLIER);
        backOff.setMaxInterval(MAX_INTERVAL_MS);

        return backOff;
    }

    /**
     * Registra tentativas, envio à DLT e falhas de recuperação.
     */
    private static final class DeadLetterRetryListener implements RetryListener {

        @Override
        public void failedDelivery(ConsumerRecord<?, ?> record, Exception ex, int deliveryAttempt) {
            log.warn("Delivery attempt {} failed for {}-{} offset {}:{}", record.topic(), record.partition(),
                    record.offset(), record.topic(), DLT_SUFFIX, ex);
        }

        @Override
        public void recovered(ConsumerRecord<?, ?> record, Exception ex) {
            log.error("Record from {}-{} offset {} exhausted retries and was sent to {}{}.",
                    record.topic(), record.partition(), record.offset(), record.topic(), DLT_SUFFIX, ex);
        }

        @Override
        public void recoveryFailed(ConsumerRecord<?, ?> record, Exception original, Exception failure) {
            log.error("Could not publish {}-{} offset {} to the dead letter topic. The record will be retried.",
                    record.topic(), record.partition(), record.offset(), failure);
        }
    }
}
