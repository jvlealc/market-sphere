package io.github.jvlealc.marketsphere.orders.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {


    private final String bootstrapServers;
    private final String groupId;
    private final String outOffsetReset;

    public KafkaConfig(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${spring.kafka.consumer.group-id}") String groupId,
            @Value("${spring.kafka.consumer.auto-offset-reset}") String outOffsetReset
    ) {
        this.bootstrapServers = bootstrapServers;
        this.groupId = groupId;
        this.outOffsetReset = outOffsetReset;
    }

    /**
     * Configurações do Producer
     * */
    // Configurando propriedades do produtor das mensagens | <String, String> Tipo de dado que iremos trafegar no Kafka
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);            // Definir qual o servidor o Kafka está rodando
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);   // Definir o tipo de classe que irá configurar a serialização das chaves
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class); // Definir o tipo de classe que irá configurar a serialização dos valores

        return new DefaultKafkaProducerFactory<>(props);
    }

    // Realiza a publicação
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Configurações do Consumer
     * */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();

        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, outOffsetReset);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);   // Definir o tipo de classe que irá configurar a desserialização das chaves
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class); // Definir o tipo de classe que irá configurar a desserialização dos valores

        return new DefaultKafkaConsumerFactory<>(props);
    }

    // Definir como as mensagens do broker serão consumidas
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<String, String> listener = new ConcurrentKafkaListenerContainerFactory<>();

        listener.setConsumerFactory(consumerFactory);

        return listener;
    }
}
