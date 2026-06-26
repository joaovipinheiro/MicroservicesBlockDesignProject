package br.com.arenamanager.tournament_service.Dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RuleSetRequest {

    @NotBlank(message = "O formato do torneio é obrigatório")
    private String format;

    @NotNull(message = "O número máximo de participantes é obrigatório")
    @Min(value = 2, message = "O torneio deve ter pelo menos 2 participantes")
    private Integer maxParticipants;

    @NotNull(message = "O bestOf é obrigatório")
    @Min(value = 1, message = "O bestOf deve ser pelo menos 1")
    private Integer bestOf;
}