package br.com.arenamanager.notification_service.infrastructure.kafka.event;

import java.math.BigDecimal;

/**
 * Evento publicado pelo payment-service no tópico {@code payments-approved}.
 * Os campos seguem o contrato definido pelo payment-service.
 */
public record PaymentApprovedEvent(
        Long paymentId,
        String playerName,
        String playerEmail,
        Long tournamentId,
        BigDecimal amount
) {}
