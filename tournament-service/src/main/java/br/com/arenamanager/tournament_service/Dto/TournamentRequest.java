package br.com.arenamanager.tournament_service.Dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TournamentRequest {

    @NotBlank(message = "O nome do torneio não pode estar em branco")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String name;

    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String description;

    @NotNull(message = "A data de início é obrigatória")
    private LocalDateTime startDate;

    @NotNull(message = "A data de fim é obrigatória")
    private LocalDateTime endDate;

    @NotNull(message = "As regras do torneio são obrigatórias")
    @Valid
    private RuleSetRequest ruleSet;
}
