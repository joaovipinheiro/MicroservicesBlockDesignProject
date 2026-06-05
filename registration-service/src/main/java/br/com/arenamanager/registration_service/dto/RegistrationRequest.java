package br.com.arenamanager.registration_service.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegistrationRequest {

    @NotNull(message = "playerId é obrigatório")
    private Long playerId;

    @NotNull(message = "tournamentId é obrigatório")
    private Long tournamentId;

    @NotBlank(message = "metodoPagamento é obrigatório")
    private String metodoPagamento;

    @NotNull(message = "valor é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "valor deve ser maior que zero")
    private BigDecimal valor;
}
