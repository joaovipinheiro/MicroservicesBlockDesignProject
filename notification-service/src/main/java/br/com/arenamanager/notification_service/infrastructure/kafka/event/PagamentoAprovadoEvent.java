package br.com.arenamanager.notification_service.infrastructure.kafka.event;

import java.math.BigDecimal;
import java.time.Instant;

public record PagamentoAprovadoEvent(
        String eventId,
        String paymentId,
        String playerId,
        String playerEmail,
        String playerName,
        String tournamentId,
        String tournamentName,
        BigDecimal amount,
        String currency,
        Instant approvedAt,
        String traceId
) {}
