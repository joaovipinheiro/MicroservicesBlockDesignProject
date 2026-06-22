package br.com.arenamanager.tournament_service.Dto;

import br.com.arenamanager.tournament_service.domain.model.TournamentStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TournamentResponse {
    private Long id;
    private String name;
    private String description;
    private TournamentStatus status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
}