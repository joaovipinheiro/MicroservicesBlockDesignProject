package br.com.arenamanager.tournament_service.Dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TournamentCreatedEvent {
    private Long idTorneio;
    private String nome;
    private String formato;
}
