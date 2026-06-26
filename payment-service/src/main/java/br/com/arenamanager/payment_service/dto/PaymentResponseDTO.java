package br.com.arenamanager.payment_service.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponseDTO(
        Long id,
        Long playerId,
        Long tournamentId,
        BigDecimal amount,
        String status,
        LocalDateTime createdAt
) {
}
