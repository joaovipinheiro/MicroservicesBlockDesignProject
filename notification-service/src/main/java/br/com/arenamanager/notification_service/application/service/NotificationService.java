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
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

/**
 * Serviço de aplicação responsável por processar notificações de pagamento aprovado.
 *
 * <p>Implementa o fluxo de idempotência, construção do e-mail, envio e persistência
 * do log de notificação com as métricas correspondentes.</p>
 *
 * <p>Valida: Requisitos 1.1, 2.1, 2.2, 3.1, 4.1, 5.1</p>
 */
@Service
public class NotificationService implements ProcessPaymentNotificationUseCase {

    private static final String COUNTER_SENT      = "notifications.sent.total";
    private static final String COUNTER_FAILED    = "notifications.failed.total";
    private static final String COUNTER_DUPLICATE = "notifications.duplicate.total";

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

    /**
     * Processa a notificação de pagamento aprovado.
     *
     * <ul>
     *   <li>Se o evento já foi processado (duplicate), persiste log com status DUPLICATE e retorna.</li>
     *   <li>Caso contrário, constrói e envia o e-mail. Em caso de sucesso, persiste log SENT.</li>
     *   <li>Qualquer exceção durante a construção ou envio faz com que o log FAILED seja persistido
     *       e a exceção seja relançada para o mecanismo de retry/DLQ do Kafka.</li>
     *   <li>Se a própria persistência do log lançar {@link com.mongodb.MongoException},
     *       a exceção é propagada sem ser capturada.</li>
     * </ul>
     *
     * @param event evento de pagamento aprovado consumido do Kafka
     */
    @Override
    public void processPaymentApprovedNotification(PaymentApprovedEvent event) {

        // --- Idempotência: verificar se o evento já foi processado com sucesso ---
        String eventId = String.valueOf(event.paymentId());
        Optional<NotificationLog> existingLog = logRepository.findByEventId(eventId);
        if (existingLog.isPresent() &&
                (existingLog.get().status() == NotificationStatus.SENT ||
                 existingLog.get().status() == NotificationStatus.DUPLICATE)) {
            NotificationLog duplicateLog = buildLog(event, NotificationStatus.DUPLICATE, null);
            logRepository.save(duplicateLog);
            meterRegistry.counter(COUNTER_DUPLICATE).increment();
            return;
        }

        // --- Fluxo principal: construir e enviar o e-mail ---
        EmailMessage emailMessage;
        try {
            emailMessage = emailBuilderService.build(event);
            emailSender.send(emailMessage);
        } catch (RuntimeException ex) {
            // Persiste o log de falha apenas se ainda não houver um log para este evento
            // (evita conflito de índice único em retentativas do Kafka)
            if (!logRepository.existsByEventId(String.valueOf(event.paymentId()))) {
                NotificationLog failedLog = buildLog(event, NotificationStatus.FAILED, ex.getMessage());
                logRepository.save(failedLog);   // MongoException aqui propaga diretamente
            }
            meterRegistry.counter(COUNTER_FAILED).increment();
            throw ex;
        }

        // --- Sucesso: persiste log SENT ---
        NotificationLog sentLog = buildLog(event, NotificationStatus.SENT, null);
        logRepository.save(sentLog);         // MongoException aqui propaga diretamente
        meterRegistry.counter(COUNTER_SENT).increment();
    }

    // -------------------------------------------------------------------------
    // Helpers privados
    // -------------------------------------------------------------------------

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
