package br.com.arenamanager.notification_service.infrastructure.mongodb.repository;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

/**
 * Spring Data MongoDB repository for the {@code notification_logs} collection.
 *
 * <p>Valida: Requisitos 5.1, 5.2, 7.2</p>
 */
public interface NotificationLogMongoRepository
        extends MongoRepository<NotificationLogDocument, String> {

    Optional<NotificationLogDocument> findByEventId(String eventId);

    boolean existsByEventId(String eventId);

    Page<NotificationLogDocument> findByPlayerIdOrderBySentAtDesc(String playerId, Pageable pageable);
}
