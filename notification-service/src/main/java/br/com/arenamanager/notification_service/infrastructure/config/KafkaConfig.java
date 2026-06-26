package br.com.arenamanager.notification_service.infrastructure.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties.AckMode;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Kafka configuration for the notification service.
 *
 * <p>Configures:
 * <ul>
 *   <li>{@link ConcurrentKafkaListenerContainerFactory} with {@code AckMode.MANUAL_IMMEDIATE}</li>
 *   <li>{@link DefaultErrorHandler} with {@link ExponentialBackOff} (3 retries, 1s initial, factor 2, max 10s)</li>
 *   <li>{@link DeadLetterPublishingRecoverer} pointing to the configured DLQ topic</li>
 * </ul>
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    private static final int DEFAULT_MAX_RETRIES = 3;
    private static final long DEFAULT_INITIAL_INTERVAL_MS = 1_000L;
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final long DEFAULT_MAX_INTERVAL_MS = 10_000L;

    @Value("${notification.kafka.topic.dlq:pagamentos.aprovados.dlq}")
    private String dlqTopic;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id:notification-service-group}")
    private String consumerGroupId;

    @Value("${spring.kafka.consumer.auto-offset-reset:earliest}")
    private String autoOffsetReset;

    @Value("${notification.kafka.retry.max-attempts:3}")
    private int maxRetries;

    @Value("${notification.kafka.retry.initial-interval-ms:1000}")
    private long initialIntervalMs;

    @Value("${notification.kafka.retry.multiplier:2.0}")
    private double multiplier;

    @Value("${notification.kafka.retry.max-interval-ms:10000}")
    private long maxIntervalMs;

    /**
     * Creates a {@link ConsumerFactory} for {@code String} keys and values.
     */
    @Bean
    public ConsumerFactory<String, String> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    /**
     * Creates a {@link ProducerFactory} for {@code String} keys and values.
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return new DefaultKafkaProducerFactory<>(props);
    }

    /**
     * Creates the {@link KafkaTemplate} bean used to send messages (e.g., DLQ routing).
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(ProducerFactory<String, String> producerFactory) {
        return new KafkaTemplate<>(producerFactory);
    }

    /**
     * Creates the {@link ConcurrentKafkaListenerContainerFactory} bean used by
     * {@code @KafkaListener} methods.
     *
     * <p>Sets {@code AckMode.MANUAL_IMMEDIATE} so that consumers must explicitly call
     * {@code Acknowledgment#acknowledge()} before the offset is committed, and registers
     * the {@link DefaultErrorHandler} for retry and DLQ routing.
     *
     * @param consumerFactory auto-configured Spring Kafka consumer factory
     * @param errorHandler    the configured {@link DefaultErrorHandler} bean
     * @return configured container factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties().setAckMode(AckMode.MANUAL_IMMEDIATE);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

    /**
     * Creates the {@link DefaultErrorHandler} bean with exponential backoff and DLQ recovery.
     *
     * <p>Retry policy:
     * <ul>
     *   <li>Max retries: {@value MAX_RETRIES} (4 total attempts)</li>
     *   <li>Initial interval: {@value INITIAL_INTERVAL_MS} ms</li>
     *   <li>Multiplier: {@value MULTIPLIER}</li>
     *   <li>Max interval: {@value MAX_INTERVAL_MS} ms</li>
     * </ul>
     *
     * <p>After all retries are exhausted the message is forwarded to {@code dlqTopic}
     * (defaults to {@code pagamentos.aprovados.dlq}) via
     * {@link DeadLetterPublishingRecoverer}.
     *
     * @param kafkaTemplate used internally by {@link DeadLetterPublishingRecoverer}
     * @return configured error handler
     */
    @Bean
    public DefaultErrorHandler defaultErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        ExponentialBackOff backOff = new ExponentialBackOff(initialIntervalMs, multiplier);
        backOff.setMaxAttempts(maxRetries);
        backOff.setMaxInterval(maxIntervalMs);

        BiFunction<ConsumerRecord<?, ?>, Exception, org.apache.kafka.common.TopicPartition> destinationResolver =
                (record, ex) -> new org.apache.kafka.common.TopicPartition(dlqTopic, -1);

        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(kafkaTemplate, destinationResolver);

        return new DefaultErrorHandler(recoverer, backOff);
    }
}
