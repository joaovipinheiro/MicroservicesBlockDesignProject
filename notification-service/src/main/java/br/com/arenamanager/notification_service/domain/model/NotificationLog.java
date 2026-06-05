package br.com.arenamanager.notification_service.domain.model;

import br.com.arenamanager.notification_service.domain.enums.NotificationStatus;
import java.time.Instant;

public record NotificationLog(
        String id,
        String eventId,
        String paymentId,
        String playerId,
        String playerEmail,
        NotificationStatus status,
        String errorMessage,
        Instant sentAt,
        String traceId
) {}
