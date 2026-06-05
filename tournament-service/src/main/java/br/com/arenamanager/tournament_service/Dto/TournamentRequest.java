package br.com.arenamanager.tournament_service.Dto;

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
    private LocalDateTime data_inicio;
    private LocalDateTime data_fim;
    private RuleSetRequest regras;
}
