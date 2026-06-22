package br.com.arenamanager.registration_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PaymentRequest {
    private Long playerId;
    private Long tournamentId;
    private BigDecimal amount;
}
