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
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;
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

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationLogRepository logRepository;
    @Mock private EmailTemplateRepository templateRepository;
    @Mock private EmailSenderPort emailSender;
    @Mock private EmailBuilderService emailBuilderService;

    private SimpleMeterRegistry meterRegistry;
    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        notificationService = new NotificationService(
                logRepository, templateRepository, emailSender, meterRegistry, emailBuilderService);
    }

    // Cenário 1: Evento novo → e-mail enviado e log SENT
    @Test
    void whenNewEvent_thenEmailSentAndLogSavedWithStatusSent() {
        PaymentApprovedEvent event = buildEvent();
        EmailMessage emailMessage = new EmailMessage("jogador@exemplo.com", "Assunto", "<h1>Olá!</h1>", "1");

        when(logRepository.findByEventId("1")).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.processPaymentApprovedNotification(event);

        verify(emailSender, times(1)).send(emailMessage);
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());
        assertEquals(NotificationStatus.SENT, captor.getValue().status());
        assertEquals("1", captor.getValue().eventId());
        assertEquals("jogador@exemplo.com", captor.getValue().playerEmail());
        assertNull(captor.getValue().errorMessage());
        assertNotNull(captor.getValue().sentAt());
    }

    // Cenário 2: Evento duplicado → log DUPLICATE, sem envio
    @Test
    void whenDuplicateEvent_thenLogSavedWithStatusDuplicate() {
        PaymentApprovedEvent event = buildEvent();
        NotificationLog existing = new NotificationLog(
                "id", "1", "1", "2", "jogador@exemplo.com",
                NotificationStatus.SENT, null, Instant.now(), "1");

        when(logRepository.findByEventId("1")).thenReturn(java.util.Optional.of(existing));
        when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        notificationService.processPaymentApprovedNotification(event);

        verify(emailSender, never()).send(any());
        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());
        assertEquals(NotificationStatus.DUPLICATE, captor.getValue().status());
        assertEquals("1", captor.getValue().eventId());
    }

    // Cenário 3: Template não encontrado → log FAILED, exceção propagada
    @Test
    void whenTemplateNotFound_thenLogSavedWithStatusFailedAndExceptionPropagated() {
        PaymentApprovedEvent event = buildEvent();
        when(logRepository.findByEventId("1")).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenThrow(new TemplateNotFoundException("PAGAMENTO_APROVADO"));
        when(logRepository.existsByEventId("1")).thenReturn(false);
        when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(TemplateNotFoundException.class,
                () -> notificationService.processPaymentApprovedNotification(event));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());
        assertEquals(NotificationStatus.FAILED, captor.getValue().status());
        assertNotNull(captor.getValue().errorMessage());
    }

    // Cenário 4: MailException → log FAILED, exceção propagada
    @Test
    void whenMailExceptionThrown_thenLogSavedWithStatusFailedAndExceptionPropagated() {
        PaymentApprovedEvent event = buildEvent();
        EmailMessage emailMessage = new EmailMessage("jogador@exemplo.com", "Assunto", "<h1>Olá!</h1>", "1");

        when(logRepository.findByEventId("1")).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        doThrow(new MailSendException("SMTP unavailable")).when(emailSender).send(emailMessage);
        when(logRepository.existsByEventId("1")).thenReturn(false);
        when(logRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        assertThrows(MailSendException.class,
                () -> notificationService.processPaymentApprovedNotification(event));

        ArgumentCaptor<NotificationLog> captor = ArgumentCaptor.forClass(NotificationLog.class);
        verify(logRepository, times(1)).save(captor.capture());
        assertEquals(NotificationStatus.FAILED, captor.getValue().status());
        assertNotNull(captor.getValue().errorMessage());
    }

    // Cenário 5: MongoException ao salvar → exceção propagada
    @Test
    void whenMongoExceptionOnSave_thenExceptionPropagated() {
        PaymentApprovedEvent event = buildEvent();
        EmailMessage emailMessage = new EmailMessage("jogador@exemplo.com", "Assunto", "<h1>Olá!</h1>", "1");

        when(logRepository.findByEventId("1")).thenReturn(java.util.Optional.empty());
        when(emailBuilderService.build(event)).thenReturn(emailMessage);
        doNothing().when(emailSender).send(emailMessage);
        when(logRepository.save(any())).thenThrow(new com.mongodb.MongoException("Connection refused"));

        assertThrows(com.mongodb.MongoException.class,
                () -> notificationService.processPaymentApprovedNotification(event));
    }

    private PaymentApprovedEvent buildEvent() {
        return new PaymentApprovedEvent(1L, "João da Silva", "jogador@exemplo.com", 2L, new BigDecimal("99.90"));
    }
}
