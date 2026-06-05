package br.com.arenamanager.notification_service.application.service;

import br.com.arenamanager.notification_service.application.port.out.EmailSenderPort;
import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.application.port.out.NotificationLogRepository;
import br.com.arenamanager.notification_service.domain.enums.NotificationStatus;
import br.com.arenamanager.notification_service.domain.exception.TemplateNotFoundException;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.EmailTemplate;
import br.com.arenamanager.notification_service.domain.model.NotificationLog;
import br.com.arenamanager.notification_service.infrastructure.email.EmailBuilderService;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PagamentoAprovadoEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Testes unitários para {@link NotificationService}.
 *
 * <p>Valida: Requisitos 1.1, 2.1, 3.2, 4.2, 5.1, 5.3</p>
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationLogRepository logRepository;

    @Mock
    private EmailTemplateRepository templateRepository;

    @Mock
    private EmailSenderPort emailSender;

    @Mock
    private EmailBuilderService emailBuilderService;

    private SimpleMeterRegistry meterRegistry;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationService(
                logRepository,
                templateRepository,
                emailSender,
                meterRegistry,
                emailBuilderService
        );
    }

    // -------------------------------------------------------------------------
    // Cenário 1: Evento novo — e-mail enviado e log salvo com status SENT
    // Valida: Requisito 1.1, 4.2, 5.1
    // -------------------------------------------------------------------------

    @Test
    void whenNewEvent_thenEmailSentAndLogSavedWithStatusSent() {
        // Given
        PagamentoAprovadoEvent event = buildEvent();
        EmailTemplate template = new EmailTemplate("tmpl-1", "PAGAMENTO_APROVADO",
                "Pagamento Aprovado", "<h1>Olá!</h1>");
        EmailMessage emailMessage = new EmailMessage(
                event.playerEmail(), template.subject(), "<h1>Olá!</h1>", event.traceId());

        when(logRepository.findByEventId(event.eventId())).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.processPaymentApprovedNotification(event);

        // Then
        verify(emailSender, times(1)).send(emailMessage);

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(NotificationStatus.SENT, savedLog.status());
        assertEquals(event.eventId(), savedLog.eventId());
        assertEquals(event.paymentId(), savedLog.paymentId());
        assertEquals(event.playerId(), savedLog.playerId());
        assertEquals(event.playerEmail(), savedLog.playerEmail());
        assertEquals(event.traceId(), savedLog.traceId());
        assertNull(savedLog.errorMessage());
        assertNotNull(savedLog.sentAt());
    }

    // -------------------------------------------------------------------------
    // Cenário 2: Evento duplicado — log salvo com status DUPLICATE, sem envio
    // Valida: Requisito 2.1
    // -------------------------------------------------------------------------

    @Test
    void whenDuplicateEvent_thenLogSavedWithStatusDuplicate() {
        // Given
        PagamentoAprovadoEvent event = buildEvent();
        NotificationLog existingLog = new NotificationLog(
                "log-id", event.eventId(), event.paymentId(), event.playerId(),
                event.playerEmail(), NotificationStatus.SENT, null, Instant.now(), event.traceId());
        when(logRepository.findByEventId(event.eventId())).thenReturn(java.util.Optional.of(existingLog));
        when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        notificationService.processPaymentApprovedNotification(event);

        // Then
        verify(emailSender, never()).send(any());

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(NotificationStatus.DUPLICATE, savedLog.status());
        assertEquals(event.eventId(), savedLog.eventId());
    }

    // -------------------------------------------------------------------------
    // Cenário 3: Template não encontrado — log FAILED, exceção propagada
    // Valida: Requisito 3.2
    // -------------------------------------------------------------------------

    @Test
    void whenTemplateNotFound_thenLogSavedWithStatusFailedAndExceptionPropagated() {
        // Given
        PagamentoAprovadoEvent event = buildEvent();
        when(logRepository.findByEventId(event.eventId())).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event))
                .thenThrow(new TemplateNotFoundException("PAGAMENTO_APROVADO"));
        when(logRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When / Then
        assertThrows(TemplateNotFoundException.class,
                () -> notificationService.processPaymentApprovedNotification(event));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedLog.status());
        assertEquals(event.eventId(), savedLog.eventId());
        assertNotNull(savedLog.errorMessage());
    }

    // -------------------------------------------------------------------------
    // Cenário 4: MailException ao enviar — log FAILED, exceção propagada
    // Valida: Requisito 4.2
    // -------------------------------------------------------------------------

    @Test
    void whenMailExceptionThrown_thenLogSavedWithStatusFailedAndExceptionPropagated() {
        // Given
        PagamentoAprovadoEvent event = buildEvent();
        EmailTemplate template = new EmailTemplate("tmpl-1", "PAGAMENTO_APROVADO",
                "Pagamento Aprovado", "<h1>Olá!</h1>");
        EmailMessage emailMessage = new EmailMessage(
                event.playerEmail(), template.subject(), "<h1>Olá!</h1>", event.traceId());

        when(logRepository.findByEventId(event.eventId())).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        doThrow(new MailSendException("SMTP server unavailable"))
                .when(emailSender).send(emailMessage);
        when(logRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(logRepository.save(any(NotificationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When / Then
        assertThrows(MailSendException.class,
                () -> notificationService.processPaymentApprovedNotification(event));

        ArgumentCaptor<NotificationLog> logCaptor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(logCaptor.capture());
        NotificationLog savedLog = logCaptor.getValue();
        assertEquals(NotificationStatus.FAILED, savedLog.status());
        assertEquals(event.eventId(), savedLog.eventId());
        assertNotNull(savedLog.errorMessage());
    }

    // -------------------------------------------------------------------------
    // Cenário 5: MongoException ao salvar o log — exceção propagada
    // Valida: Requisito 5.3
    // -------------------------------------------------------------------------

    @Test
    void whenMongoExceptionOnSave_thenExceptionPropagated() {
        // Given
        PagamentoAprovadoEvent event = buildEvent();
        EmailTemplate template = new EmailTemplate("tmpl-1", "PAGAMENTO_APROVADO",
                "Pagamento Aprovado", "<h1>Olá!</h1>");
        EmailMessage emailMessage = new EmailMessage(
                event.playerEmail(), template.subject(), "<h1>Olá!</h1>", event.traceId());

        when(logRepository.findByEventId(event.eventId())).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        doNothing().when(emailSender).send(emailMessage);
        when(logRepository.save(any(NotificationLog.class)))
                .thenThrow(new com.mongodb.MongoException("Connection refused"));

        // When / Then
        assertThrows(com.mongodb.MongoException.class,
                () -> notificationService.processPaymentApprovedNotification(event));
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private PagamentoAprovadoEvent buildEvent() {
        return new PagamentoAprovadoEvent(
                "evt-uuid-0001",
                "pay-uuid-0001",
                "player-uuid-0001",
                "jogador@exemplo.com",
                "João da Silva",
                "tournament-uuid-0001",
                "Copa Arena 2026",
                new BigDecimal("99.90"),
                "BRL",
                Instant.parse("2026-01-15T10:30:00Z"),
                "trace-abc123"
        );
    }
}
