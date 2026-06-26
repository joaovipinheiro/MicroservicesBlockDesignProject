package br.com.arenamanager.notification_service.application.port.out;

import br.com.arenamanager.notification_service.domain.model.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface NotificationLogRepository {

    NotificationLog save(NotificationLog log);

    Optional<NotificationLog> findByEventId(String eventId);

    Page<NotificationLog> findByPlayerIdOrderBySentAtDesc(String playerId, Pageable pageable);

    boolean existsByEventId(String eventId);
}
