package br.com.arenamanager.notification_service.application.service;

import br.com.arenamanager.notification_service.application.port.in.ProcessPaymentNotificationUseCase;
import br.com.arenamanager.notification_service.application.port.out.EmailSenderPort;
import br.com.arenamanager.notification_service.application.port.out.EmailTemplateRepository;
import br.com.arenamanager.notification_service.application.port.out.NotificationLogRepository;
import br.com.arenamanager.notification_service.domain.enums.NotificationStatus;
import br.com.arenamanager.notification_service.domain.model.EmailMessage;
import br.com.arenamanager.notification_service.domain.model.NotificationLog;
import br.com.arenamanager.notification_service.infrastructure.email.EmailBuilderService;
import br.com.arenamanager.notification_service.infrastructure.kafka.event.PaymentApprovedEvent;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class NotificationService implements ProcessPaymentNotificationUseCase {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private static final String COUNTER_SENT      = "notifications.sent.total";
    private static final String COUNTER_FAILED    = "notifications.failed.total";
    private static final String COUNTER_DUPLICATE = "notifications.duplicate.total";
    private static final String TIMER_PROCESSING  = "notifications.processing.duration";

    private final NotificationLogRepository logRepository;
    private final EmailTemplateRepository templateRepository;
    private final EmailSenderPort emailSender;
    private final MeterRegistry meterRegistry;
    private final EmailBuilderService emailBuilderService;

    public NotificationService(NotificationLogRepository logRepository,
                               EmailTemplateRepository templateRepository,
                               EmailSenderPort emailSender,
                               MeterRegistry meterRegistry,
                               EmailBuilderService emailBuilderService) {
        this.logRepository       = logRepository;
        this.templateRepository  = templateRepository;
        this.emailSender         = emailSender;
        this.meterRegistry       = meterRegistry;
        this.emailBuilderService = emailBuilderService;
    }

    @Override
    public void processPaymentApprovedNotification(PaymentApprovedEvent event) {
        String eventId = String.valueOf(event.paymentId());
        long inicio = System.currentTimeMillis();

        log.info("Iniciando processamento de notificação: eventId={}, playerEmail={}", eventId, event.playerEmail());

        // --- Idempotência ---
        Optional<NotificationLog> existingLog = logRepository.findByEventId(eventId);
        if (existingLog.isPresent() &&
                (existingLog.get().status() == NotificationStatus.SENT ||
                 existingLog.get().status() == NotificationStatus.DUPLICATE)) {
            log.warn("Evento duplicado ignorado: eventId={}, playerEmail={}", eventId, event.playerEmail());
            NotificationLog duplicateLog = buildLog(event, NotificationStatus.DUPLICATE, null);
            logRepository.save(duplicateLog);
            meterRegistry.counter(COUNTER_DUPLICATE).increment();
            return;
        }

        // --- Fluxo principal ---
        EmailMessage emailMessage;
        try {
            emailMessage = emailBuilderService.build(event);
            emailSender.send(emailMessage);
        } catch (RuntimeException ex) {
            log.error("Falha ao processar notificação: eventId={}, playerEmail={}, erro={}",
                    eventId, event.playerEmail(), ex.getMessage());
            if (!logRepository.existsByEventId(eventId)) {
                NotificationLog failedLog = buildLog(event, NotificationStatus.FAILED, ex.getMessage());
                logRepository.save(failedLog);
            }
            meterRegistry.counter(COUNTER_FAILED).increment();
            meterRegistry.timer(TIMER_PROCESSING)
                    .record(System.currentTimeMillis() - inicio, TimeUnit.MILLISECONDS);
            throw ex;
        }

        // --- Sucesso ---
        NotificationLog sentLog = buildLog(event, NotificationStatus.SENT, null);
        logRepository.save(sentLog);
        meterRegistry.counter(COUNTER_SENT).increment();
        meterRegistry.timer(TIMER_PROCESSING)
                .record(System.currentTimeMillis() - inicio, TimeUnit.MILLISECONDS);

        log.info("Notificação processada com sucesso: eventId={}, playerEmail={}, duracaoMs={}",
                eventId, event.playerEmail(), System.currentTimeMillis() - inicio);
    }

    private NotificationLog buildLog(PaymentApprovedEvent event,
                                     NotificationStatus status,
                                     String errorMessage) {
        return new NotificationLog(
                null,
                String.valueOf(event.paymentId()),
                String.valueOf(event.paymentId()),
                String.valueOf(event.tournamentId()),
                event.playerEmail(),
                status,
                errorMessage,
                Instant.now(),
                String.valueOf(event.paymentId())
        );
    }
}
