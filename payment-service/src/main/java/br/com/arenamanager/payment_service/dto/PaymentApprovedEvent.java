package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;

public record PaymentApprovedEvent(
        Long paymentId,
        String playerName,
        String playerEmail,
        Long tournamentId,
        BigDecimal amount
) {}