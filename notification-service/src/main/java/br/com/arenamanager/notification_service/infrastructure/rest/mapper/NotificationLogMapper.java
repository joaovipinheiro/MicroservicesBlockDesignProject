package br.com.arenamanager.notification_service.infrastructure.rest.mapper;

import br.com.arenamanager.notification_service.infrastructure.mongodb.document.NotificationLogDocument;
import br.com.arenamanager.notification_service.infrastructure.rest.dto.NotificationLogResponse;
import org.springframework.stereotype.Component;

/**
 * Mapper that converts a {@link NotificationLogDocument} to a {@link NotificationLogResponse}.
 *
 * <p>Valida: Requisito 7.2</p>
 */
@Component
public class NotificationLogMapper {

    /**
     * Maps a MongoDB document to its REST response representation.
     *
     * @param doc the source document; must not be {@code null}
     * @return the mapped {@link NotificationLogResponse}
     */
    public NotificationLogResponse toResponse(NotificationLogDocument doc) {
        String sentAt = doc.getSentAt() != null ? doc.getSentAt().toString() : null;

        return new NotificationLogResponse(
                doc.getEventId(),
                doc.getPaymentId(),
                doc.getPlayerEmail(),
                doc.getStatus(),
                doc.getErrorMessage(),
                sentAt,
                doc.getTraceId()
        );
    }
}
