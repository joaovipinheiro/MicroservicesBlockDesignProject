package br.com.arenamanager.registration_service.dto;

import br.com.arenamanager.registration_service.domain.model.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegistrationRequest {

    @NotNull(message = "playerId é obrigatório")
    private Long playerId;

    @NotNull(message = "tournamentId é obrigatório")
    private Long tournamentId;

    @NotNull(message = "paymentMethod é obrigatório")
    private PaymentMethod paymentMethod;

    @NotNull(message = "amount é obrigatório")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount deve ser maior que zero")
    private BigDecimal amount;
}
