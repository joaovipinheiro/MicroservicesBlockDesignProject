package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;

public record PaymentRequestDTO(
        Long playerId,
        Long tournamentId,
        BigDecimal amount
) {}