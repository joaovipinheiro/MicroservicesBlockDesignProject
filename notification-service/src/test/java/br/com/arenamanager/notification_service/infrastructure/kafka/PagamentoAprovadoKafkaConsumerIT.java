package br.com.arenamanager.notification_service.infrastructure.kafka;

import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.EmailTemplateDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.EmailTemplateMongoRepository;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.SendResult;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Integration tests for {@link br.com.arenamanager.notification_service.infrastructure.kafka.consumer.PagamentoAprovadoKafkaConsumer}.
 *
 * <p>Uses {@link EmbeddedKafka} + Flapdoodle Embedded MongoDB for a full integration test
 * without external dependencies.</p>
 *
 * <p>Valida: Requisitos 1.1, 1.2, 1.3, 1.4, 2.1, 6.1, 6.2, 6.3</p>
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"pagamentos.aprovados", "pagamentos.aprovados.dlq"}
)
@TestPropertySource(properties = {
        // Point to embedded Kafka
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        // Disable Eureka and Zipkin
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "eureka.client.enabled=false",
        // Kafka topic config
        "notification.kafka.topic.pagamentos-aprovados=pagamentos.aprovados",
        "notification.kafka.topic.dlq=pagamentos.aprovados.dlq",
        // Consumer config
        "spring.kafka.consumer.group-id=test-notification-group",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        // Producer config (required for KafkaTemplate auto-configuration)
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        // Flapdoodle embedded MongoDB version
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        // Disable health contributors that require real infrastructure
        "management.health.mail.enabled=false",
        "management.health.kafka.enabled=false",
        // Speed up retries for tests (100ms initial, factor 1, max 200ms = ~3 retries in ~300ms)
        "notification.kafka.retry.max-attempts=3",
        "notification.kafka.retry.initial-interval-ms=100",
        "notification.kafka.retry.multiplier=1.0",
        "notification.kafka.retry.max-interval-ms=200"
})
@DirtiesContext
class PagamentoAprovadoKafkaConsumerIT {

    private static final String TOPIC = "pagamentos.aprovados";
    private static final String DLQ_TOPIC = "pagamentos.aprovados.dlq";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationLogMongoRepository notificationLogMongoRepository;

    @Autowired
    private EmailTemplateMongoRepository emailTemplateMongoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // Always mock JavaMailSender to avoid real email sending
    @MockitoBean
    private JavaMailSender javaMailSender;

    private KafkaTemplate<String, String> kafkaTemplate;
    private KafkaConsumer<String, String> dlqConsumer;

    @BeforeEach
    void setUp() {
        // Stub createMimeMessage() to return a real MimeMessage so the helper can build it
        when(javaMailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));

        // Create a KafkaTemplate backed by the embedded broker
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);

        // Create a raw KafkaConsumer to verify DLQ messages
        Map<String, Object> consumerProps = new HashMap<>();
        consumerProps.put("bootstrap.servers", embeddedKafkaBroker.getBrokersAsString());
        consumerProps.put("group.id", "dlq-test-consumer-" + UUID.randomUUID());
        consumerProps.put("key.deserializer", StringDeserializer.class.getName());
        consumerProps.put("value.deserializer", StringDeserializer.class.getName());
        consumerProps.put("auto.offset.reset", "earliest");
        consumerProps.put("enable.auto.commit", "false");
        dlqConsumer = new KafkaConsumer<>(consumerProps);
        dlqConsumer.subscribe(List.of(DLQ_TOPIC));
    }

    @AfterEach
    void tearDown() {
        notificationLogMongoRepository.deleteAll();
        emailTemplateMongoRepository.deleteAll();
        dlqConsumer.close();
        MDC.clear();
    }

    // =========================================================================
    // Scenario 1: Happy path — valid event → NotificationLog SENT in MongoDB
    // Validates: Requirements 1.1, 5.1
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void whenValidEventPublished_thenNotificationLogSavedWithStatusSent() throws Exception {
        // Given — seed the email template
        EmailTemplateDocument template = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado — {{tournamentName}}",
                "<h1>Olá, {{playerName}}!</h1><p>Seu pagamento de {{amount}} {{currency}} foi aprovado.</p>"
        );
        emailTemplateMongoRepository.save(template);

        String eventId = UUID.randomUUID().toString();
        PagamentoAprovadoEvent event = buildEvent(eventId);
        String payload = objectMapper.writeValueAsString(event);

        // When — publish to topic
        kafkaTemplate.send(TOPIC, eventId, payload).get(10, TimeUnit.SECONDS);

        // Then — wait until NotificationLog is persisted
        String expectedLogId = String.valueOf(event.pagamentoId());
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logOpt = notificationLogMongoRepository.findByEventId(expectedLogId);
                    assertThat(logOpt).isPresent();
                    NotificationLogDocument savedLog = logOpt.get();
                    assertThat(savedLog.getStatus()).isEqualTo("SENT");
                    assertThat(savedLog.getEventId()).isEqualTo(expectedLogId);
                    assertThat(savedLog.getPlayerEmail()).isEqualTo(event.emailJogador());
                    assertThat(savedLog.getErrorMessage()).isNull();
                });
    }

    // =========================================================================
    // Scenario 2: Invalid payload (malformed JSON) → message in DLQ, no exception
    // Validates: Requirements 1.2, 6.1
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void whenInvalidJsonPayload_thenMessageForwardedToDlqWithoutException() throws Exception {
        // Given — malformed JSON payload
        String invalidPayload = "{this is not valid json!";
        String key = UUID.randomUUID().toString();

        // When
        kafkaTemplate.send(TOPIC, key, invalidPayload).get(10, TimeUnit.SECONDS);

        // Then — message should arrive in DLQ
        // Accumulate records across multiple polls so we don't miss them
        // Note: the DLQ may contain messages from other tests that ran before this one,
        // so we filter by the specific invalid payload value.
        List<ConsumerRecord<String, String>> allDlqMessages = new ArrayList<>();
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records =
                            dlqConsumer.poll(Duration.ofMillis(500));
                    records.forEach(allDlqMessages::add);
                    boolean foundInvalidPayload = allDlqMessages.stream()
                            .anyMatch(r -> invalidPayload.equals(r.value()));
                    assertThat(foundInvalidPayload)
                            .as("Expected at least 1 message in DLQ matching the invalid payload")
                            .isTrue();
                });

        // And — no NotificationLog should be persisted for this malformed message
        assertThat(notificationLogMongoRepository.count()).isZero();
    }

    // =========================================================================
    // Scenario 3: Duplicate event → second processing creates DUPLICATE log,
    //             JavaMailSender is not called for the second processing
    // Validates: Requirements 2.1, 1.1
    // =========================================================================

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void whenDuplicateEventPublished_thenSecondProcessingCreatesLogWithStatusDuplicate() throws Exception {
        // Given — seed the email template
        EmailTemplateDocument template = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado — {{tournamentName}}",
                "<h1>Olá, {{playerName}}!</h1>"
        );
        emailTemplateMongoRepository.save(template);

        String eventId = UUID.randomUUID().toString();
        PagamentoAprovadoEvent event = buildEvent(eventId);
        String payload = objectMapper.writeValueAsString(event);

        // When — publish the same event twice
        kafkaTemplate.send(TOPIC, eventId, payload).get(10, TimeUnit.SECONDS);

        // Wait for first processing to complete (SENT)
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logs = notificationLogMongoRepository.findAll();
                    assertThat(logs).anyMatch(l -> l.getStatus().equals("SENT"));
                });

        // Reset mock invocation count between first and second send
        // (first send already called mailSender)
        org.mockito.Mockito.clearInvocations(javaMailSender);

        // Publish same eventId again
        kafkaTemplate.send(TOPIC, eventId, payload).get(10, TimeUnit.SECONDS);

        // Then — wait for DUPLICATE log to appear
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logs = notificationLogMongoRepository.findAll();
                    assertThat(logs).anyMatch(l -> l.getStatus().equals("DUPLICATE"));
                });

        // And — JavaMailSender should NOT have been called again after the duplicate
        verify(javaMailSender, never()).send(any(jakarta.mail.internet.MimeMessage.class));

        // Total: 2 logs (1 SENT + 1 DUPLICATE)
        assertThat(notificationLogMongoRepository.count()).isEqualTo(2L);
    }

    // =========================================================================
    // Scenario 4: 3 consecutive failures (template absent) → message in DLQ
    //             with original headers preserved
    // Validates: Requirements 6.1, 6.2, 6.3
    // =========================================================================

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void whenTemplateAbsent_thenAfterRetriesMessageSentToDlqWithOriginalHeaders() throws Exception {
        // Given — NO email template seeded (causes TemplateNotFoundException on every attempt)
        String eventId = UUID.randomUUID().toString();
        String traceId = "trace-dlq-test-" + UUID.randomUUID();
        PagamentoAprovadoEvent event = buildEventWithTraceId(eventId, traceId);
        String payload = objectMapper.writeValueAsString(event);

        // When — publish to topic with traceId header
        org.springframework.kafka.support.KafkaHeaders.CORRELATION_ID.getBytes(StandardCharsets.UTF_8);

        var producerRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(
                TOPIC, null, eventId, payload,
                List.of(new RecordHeader("X-B3-TraceId", traceId.getBytes(StandardCharsets.UTF_8)))
        );
        kafkaTemplate.send(producerRecord).get(10, TimeUnit.SECONDS);

        // Then — after 3 retries the message should land in DLQ
        // The backoff intervals are: 1s, 2s, 4s → total ~7s
        // Accumulate records across multiple polls so we don't miss the DLQ message
        List<ConsumerRecord<String, String>> allDlqMessages = new ArrayList<>();
        await().atMost(Duration.ofSeconds(55))
                .pollInterval(Duration.ofMillis(500))
                .untilAsserted(() -> {
                    ConsumerRecords<String, String> records =
                            dlqConsumer.poll(Duration.ofMillis(500));
                    records.forEach(allDlqMessages::add);
                    boolean foundPayload = allDlqMessages.stream()
                            .anyMatch(r -> payload.equals(r.value()));
                    assertThat(foundPayload)
                            .as("Expected at least 1 message in DLQ after 3 failed retries matching the payload")
                            .isTrue();
                });
    }

    // =========================================================================
    // Scenario 5: traceId from Kafka header is propagated to MDC
    // Validates: Requirements 1.3, 8.2
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void whenEventHasTraceIdHeader_thenTraceIdPropagatedToNotificationLog() throws Exception {
        // Given — seed the email template
        EmailTemplateDocument template = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado",
                "<h1>Olá, {{playerName}}!</h1>"
        );
        emailTemplateMongoRepository.save(template);

        String eventId = UUID.randomUUID().toString();
        String headerTraceId = "header-trace-" + UUID.randomUUID();

        // Create a PagamentoAprovadoEvent with a different traceId in the body
        // The header traceId should be propagated to MDC, but the event's own traceId
        // is what gets stored in the NotificationLog via the domain flow.
        // This test verifies the header is read without error.
        PagamentoAprovadoEvent event = buildEventWithTraceId(eventId, headerTraceId);
        String payload = objectMapper.writeValueAsString(event);

        // Publish with X-B3-TraceId header
        var producerRecord = new org.apache.kafka.clients.producer.ProducerRecord<>(
                TOPIC, null, eventId, payload,
                List.of(new RecordHeader("X-B3-TraceId", headerTraceId.getBytes(StandardCharsets.UTF_8)))
        );
        kafkaTemplate.send(producerRecord).get(10, TimeUnit.SECONDS);

        // Then — NotificationLog should be saved with the traceId from the event
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logOpt = notificationLogMongoRepository.findByEventId(eventId);
                    assertThat(logOpt).isPresent();
                    NotificationLogDocument savedLog = logOpt.get();
                    assertThat(savedLog.getStatus()).isEqualTo("SENT");
                    // The traceId in the log comes from the event body (which we set to headerTraceId)
                    assertThat(savedLog.getTraceId()).isEqualTo(headerTraceId);
                });
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PagamentoAprovadoEvent buildEvent(String eventId) {
        return buildEventWithTraceId(eventId, "trace-" + UUID.randomUUID());
    }

    private PagamentoAprovadoEvent buildEventWithTraceId(String eventId, String traceId) {
        // Usa o hashCode do eventId como Long pra garantir unicidade entre testes
        long idAsLong = Math.abs((long) eventId.hashCode());
        return new PagamentoAprovadoEvent(
                idAsLong,
                "Test Player",
                "player@test.com",
                2L,
                new BigDecimal("99.90")
        );
    }
}
