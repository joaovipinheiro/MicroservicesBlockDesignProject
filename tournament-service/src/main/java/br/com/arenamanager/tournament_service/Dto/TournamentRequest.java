package br.com.arenamanager.tournament_service.Dto;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String nome;
    private String descricao;

    @JsonProperty("data_inicio")
    private LocalDateTime data_inicio;

    @JsonProperty("data_fim")
    private LocalDateTime data_fim;

    private RuleSetRequest regras;
}