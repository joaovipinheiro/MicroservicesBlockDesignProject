package br.com.arenamanager.notification_service.integration;

import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.EmailTemplateDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.EmailTemplateMongoRepository;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * End-to-end integration tests for the notification flow.
 *
 * <p>Publishes a {@link PagamentoAprovadoEvent} to the embedded Kafka topic and
 * verifies the full flow: Kafka consumption → email sending → NotificationLog persistence.
 *
 * <p>Uses:
 * <ul>
 *   <li>{@link EmbeddedKafka} — in-memory Kafka broker</li>
 *   <li>Flapdoodle — embedded MongoDB (auto-configured via dependency)</li>
 *   <li>{@link MockitoBean} on {@link JavaMailSender} — avoids real SMTP connections</li>
 * </ul>
 *
 * <p>Valida: Requisitos 1.1, 3.3, 4.1, 5.1
 */
@SpringBootTest
@EmbeddedKafka(
        partitions = 1,
        topics = {"pagamentos.aprovados", "pagamentos.aprovados.dlq"}
)
@TestPropertySource(properties = {
        // Point consumer to embedded Kafka
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        // Disable Eureka and Zipkin to avoid network calls
        "spring.cloud.discovery.enabled=false",
        "management.tracing.enabled=false",
        "eureka.client.enabled=false",
        // Kafka topic config
        "notification.kafka.topic.pagamentos-aprovados=pagamentos.aprovados",
        "notification.kafka.topic.dlq=pagamentos.aprovados.dlq",
        // Unique consumer group per test run to avoid offset conflicts with other tests
        "spring.kafka.consumer.group-id=e2e-test-group-${random.uuid}",
        "spring.kafka.consumer.auto-offset-reset=earliest",
        // Producer serializers
        "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
        "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer",
        // Flapdoodle embedded MongoDB version
        "de.flapdoodle.mongodb.embedded.version=7.0.0",
        // Disable health contributors that require real infrastructure
        "management.health.mail.enabled=false",
        "management.health.kafka.enabled=false",
        // Fast retry for tests: 100ms initial, factor 1, max 200ms
        "notification.kafka.retry.max-attempts=3",
        "notification.kafka.retry.initial-interval-ms=100",
        "notification.kafka.retry.multiplier=1.0",
        "notification.kafka.retry.max-interval-ms=200"
})
@DirtiesContext
class NotificationFlowIT {

    private static final String TOPIC = "pagamentos.aprovados";

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    private NotificationLogMongoRepository notificationLogMongoRepository;

    @Autowired
    private EmailTemplateMongoRepository emailTemplateMongoRepository;

    @Autowired
    private ObjectMapper objectMapper;

    /** Always mock JavaMailSender — no real SMTP connections in tests. */
    @MockitoBean
    private JavaMailSender javaMailSender;

    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setUp() {
        // Return a real MimeMessage (backed by a null Session) so MimeMessageHelper can populate it
        when(javaMailSender.createMimeMessage())
                .thenAnswer(inv -> new MimeMessage((Session) null));

        // Build a KafkaTemplate wired to the embedded broker
        Map<String, Object> producerProps = KafkaTestUtils.producerProps(embeddedKafkaBroker);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        ProducerFactory<String, String> pf = new DefaultKafkaProducerFactory<>(producerProps);
        kafkaTemplate = new KafkaTemplate<>(pf);
    }

    @AfterEach
    void tearDown() {
        notificationLogMongoRepository.deleteAll();
        emailTemplateMongoRepository.deleteAll();
    }

    // =========================================================================
    // E2E Scenario 1: Happy path
    // Publish PagamentoAprovadoEvent → NotificationLog SENT in MongoDB
    //                                → JavaMailSender.send() called exactly once
    //                                → to = event.playerEmail()
    // Validates: Requirements 1.1, 4.1, 5.1
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void whenPagamentoAprovadoEventPublished_thenNotificationLogSavedAsSentAndEmailSent()
            throws Exception {

        // Given — pre-insert the email template
        EmailTemplateDocument template = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado — {{tournamentName}}",
                "<h1>Olá, {{playerName}}!</h1><p>Pagamento de {{amount}} {{currency}} aprovado em {{approvedAt}}.</p>"
        );
        emailTemplateMongoRepository.save(template);

        String eventId = UUID.randomUUID().toString();
        String playerEmail = "jogador.e2e@arenamanager.com";
        PagamentoAprovadoEvent event = buildEvent(eventId, playerEmail);
        String payload = objectMapper.writeValueAsString(event);

        // When — publish the event to the Kafka topic
        kafkaTemplate.send(TOPIC, eventId, payload).get(10, TimeUnit.SECONDS);

        // Then — wait for NotificationLog to be persisted with status SENT
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logOpt = notificationLogMongoRepository.findByEventId(eventId);
                    assertThat(logOpt).as("NotificationLog should be persisted").isPresent();

                    NotificationLogDocument savedLog = logOpt.get();
                    assertThat(savedLog.getStatus()).isEqualTo("SENT");
                    assertThat(savedLog.getPlayerEmail()).isEqualTo(playerEmail);
                    assertThat(savedLog.getEventId()).isEqualTo(eventId);
                    assertThat(savedLog.getErrorMessage()).isNull();
                    assertThat(savedLog.getTraceId()).isNotBlank();
                });

        // And — JavaMailSender.send(MimeMessage) must have been called exactly once
        // with to = event.playerEmail()
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    // =========================================================================
    // E2E Scenario 2: Correct subject after template interpolation
    // Verifies that the MimeMessage subject contains the interpolated tournamentName
    // Validates: Requirement 3.3
    // =========================================================================

    @Test
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void whenPagamentoAprovadoEventPublished_thenEmailSubjectContainsInterpolatedTournamentName()
            throws Exception {

        // Given — template with {{tournamentName}} placeholder in subject
        String tournamentName = "Copa Arena E2E 2026";
        EmailTemplateDocument template = new EmailTemplateDocument(
                null,
                "PAGAMENTO_APROVADO",
                "Pagamento Aprovado — {{tournamentName}}",
                "<h1>Olá, {{playerName}}!</h1>"
        );
        emailTemplateMongoRepository.save(template);

        String eventId = UUID.randomUUID().toString();
        String playerEmail = "subject.test@arenamanager.com";
        PagamentoAprovadoEvent event = buildEventWithTournamentName(eventId, playerEmail, tournamentName);
        String payload = objectMapper.writeValueAsString(event);

        // Capture the MimeMessage sent so we can inspect its subject
        java.util.concurrent.atomic.AtomicReference<MimeMessage> capturedMessage =
                new java.util.concurrent.atomic.AtomicReference<>();

        org.mockito.stubbing.Answer<Void> captureAnswer = inv -> {
            capturedMessage.set(inv.getArgument(0));
            return null;
        };
        org.mockito.Mockito.doAnswer(captureAnswer)
                .when(javaMailSender).send(any(MimeMessage.class));

        // When — publish event
        kafkaTemplate.send(TOPIC, eventId, payload).get(10, TimeUnit.SECONDS);

        // Then — wait for NotificationLog SENT
        await().atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .untilAsserted(() -> {
                    var logOpt = notificationLogMongoRepository.findByEventId(eventId);
                    assertThat(logOpt).isPresent();
                    assertThat(logOpt.get().getStatus()).isEqualTo("SENT");
                });

        // And — the subject of the captured MimeMessage should contain the interpolated tournamentName
        assertThat(capturedMessage.get()).isNotNull();
        String actualSubject = capturedMessage.get().getSubject();
        assertThat(actualSubject)
                .as("Subject should contain the interpolated tournament name")
                .contains(tournamentName);
        assertThat(actualSubject)
                .as("Subject should not contain unresolved placeholders")
                .doesNotContain("{{");
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private PagamentoAprovadoEvent buildEvent(String eventId, String playerEmail) {
        return buildEventWithTournamentName(eventId, playerEmail, "Copa Arena 2026");
    }

    private PagamentoAprovadoEvent buildEventWithTournamentName(
            String eventId, String playerEmail, String tournamentName) {
        return new PagamentoAprovadoEvent(
                eventId,
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                playerEmail,
                "Jogador E2E",
                UUID.randomUUID().toString(),
                tournamentName,
                new BigDecimal("149.90"),
                "BRL",
                Instant.parse("2026-06-01T14:00:00Z"),
                "e2e-trace-" + UUID.randomUUID()
        );
    }
}
