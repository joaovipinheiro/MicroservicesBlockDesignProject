package br.com.arenamanager.registration_service.dto;

import br.com.arenamanager.registration_service.domain.model.RegistrationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class RegistrationResponse {
    private Long id;
    private Long playerId;
    private Long tournamentId;
    private String metodoPagamento;
    private BigDecimal valor;
    private RegistrationStatus status;
    private LocalDateTime createdAt;
}
