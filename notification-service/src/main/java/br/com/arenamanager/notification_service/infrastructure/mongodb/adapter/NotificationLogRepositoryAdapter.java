package br.com.arenamanager.notification_service.infrastructure.mongodb.adapter;

import br.com.arenamanager.notification_service.application.port.out.NotificationLogRepository;
import br.com.arenamanager.notification_service.domain.enums.NotificationStatus;
import br.com.arenamanager.notification_service.domain.model.NotificationLog;
import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.mongodb.repository.NotificationLogMongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adapter that bridges the domain {@link NotificationLogRepository} port with the
 * Spring Data MongoDB {@link NotificationLogMongoRepository}.
 *
 * <p>Valida: Requisitos 5.1, 7.2</p>
 */
@Component
public class NotificationLogRepositoryAdapter implements NotificationLogRepository {

    private final NotificationLogMongoRepository mongoRepository;

    public NotificationLogRepositoryAdapter(NotificationLogMongoRepository mongoRepository) {
        this.mongoRepository = mongoRepository;
    }

    @Override
    public NotificationLog save(NotificationLog log) {
        NotificationLogDocument doc = toDocument(log);
        NotificationLogDocument saved = mongoRepository.save(doc);
        return toDomain(saved);
    }

    @Override
    public Optional<NotificationLog> findByEventId(String eventId) {
        return mongoRepository.findByEventId(eventId).map(this::toDomain);
    }

    @Override
    public Page<NotificationLog> findByPlayerIdOrderBySentAtDesc(String playerId, Pageable pageable) {
        return mongoRepository.findByPlayerIdOrderBySentAtDesc(playerId, pageable)
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEventId(String eventId) {
        return mongoRepository.existsByEventId(eventId);
    }

    // -------------------------------------------------------------------------
    // Mapping helpers
    // -------------------------------------------------------------------------

    private NotificationLogDocument toDocument(NotificationLog log) {
        return new NotificationLogDocument(
                log.id(),
                log.eventId(),
                log.paymentId(),
                log.playerId(),
                log.playerEmail(),
                log.status() != null ? log.status().name() : null,
                log.errorMessage(),
                log.sentAt(),
                log.traceId()
        );
    }

    private NotificationLog toDomain(NotificationLogDocument doc) {
        return new NotificationLog(
                doc.getId(),
                doc.getEventId(),
                doc.getPaymentId(),
                doc.getPlayerId(),
                doc.getPlayerEmail(),
                doc.getStatus() != null ? NotificationStatus.valueOf(doc.getStatus()) : null,
                doc.getErrorMessage(),
                doc.getSentAt(),
                doc.getTraceId()
        );
    }
}
